(ns clj-kondo.impl.schema
  {:no-doc true}
  (:require
   [clj-kondo.hooks-api :as hooks]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.namespace :as namespace]
   [clj-kondo.impl.types.schema.core :as schema-core]
   [clj-kondo.impl.types.utils :as type-utils]
   [clj-kondo.impl.utils :as utils]))

(def ^:private primitive-schema->tag
  '{Str :string
    Int :int
    Num :number
    Bool :boolean
    Keyword :keyword
    Symbol :symbol
    Any :any})

(def ^:private primitive-tags
  (set (vals primitive-schema->tag)))

(defn- schema-core-name [ctx sym]
  (when-let [sym-ns (some-> (namespace sym) symbol)]
    (when (= 'schema.core (get (:qualify-ns (:ns ctx)) sym-ns))
      (symbol (name sym)))))

(defn- resolved-schema-tag? [t]
  (cond
    (keyword? t)
    (or (identical? :nil t)
        (contains? primitive-tags
                   (if (= "nilable" (namespace t))
                     (keyword (name t))
                     t)))
    (set? t) (and (seq t) (every? resolved-schema-tag? t))
    (map? t) (or (type-utils/enum-type? t)
                 (identical? :keys (:op t))
                 (and (type-utils/sequential-type? t)
                      (resolved-schema-tag? (:elem t))))
    :else false))

(defn- schema-var-tag [ctx sym]
  (when (and (symbol? sym) (not (namespace sym)))
    (let [ns-name (-> ctx :ns :name)
          ns (namespace/get-namespace ctx (:base-lang ctx) (:lang ctx) ns-name)
          t (get-in ns [:vars sym :type])]
      (when (resolved-schema-tag? t)
        t))))

(defn- enum-node->tag [node]
  (let [vals (map utils/sexpr (rest (:children node)))]
    (when (and (seq vals) (every? keyword? vals))
      {:type :enum :vals (set vals)})))

(declare schema-node->tag)

(defn- optional-key-node->kw [ctx node]
  (when (identical? :list (utils/tag node))
    (let [[sym-node kw-node] (:children node)
          sym (some-> sym-node utils/sexpr)]
      (when (and (symbol? sym)
                 (= 'optional-key (schema-core-name ctx sym)))
        (let [k (some-> kw-node utils/sexpr)]
          (when (keyword? k) k))))))

(defn- map-node->tag [ctx node]
  (let [children (:children node)]
    (when (even? (count children))
      (let [acc (reduce
                 (fn [m [k-node v-node]]
                   (if-let [vt (schema-node->tag ctx v-node)]
                     (let [k (utils/sexpr k-node)]
                       (cond (keyword? k)
                             (update m :req assoc k vt)
                             :else
                             (if-let [ok (optional-key-node->kw ctx k-node)]
                               (update m :opt assoc ok vt)
                               (reduced nil))))
                     (reduced nil)))
                 {:req {} :opt {}}
                 (partition 2 children))]
        (when acc
          (cond-> {:op :keys :req (:req acc)}
            (seq (:opt acc)) (assoc :opt (:opt acc))))))))

(defn- vector-node->tag [ctx node]
  (let [children (:children node)]
    (when (= 1 (count children))
      (when-let [elem (schema-node->tag ctx (first children))]
        {:type :sequential :elem elem}))))

(defn- either-node->tag [ctx node]
  (let [branches (map #(schema-node->tag ctx %) (rest (:children node)))]
    (when (and (seq branches) (every? some? branches))
      (schema-core/flatten-either-tags branches))))

(defn schema-node->tag [ctx node]
  (case (utils/tag node)
    :token (let [v (utils/sexpr node)]
             (cond (symbol? v)
                   (or (some->> (schema-core-name ctx v) (get primitive-schema->tag))
                       (schema-var-tag ctx v))))
    :list (when-let [sym (some-> node :children first utils/sexpr)]
            (when (symbol? sym)
              (case (schema-core-name ctx sym)
                enum (enum-node->tag node)
                maybe (when-let [inner (second (:children node))]
                        (schema-core/maybe-tag (schema-node->tag ctx inner)))
                either (either-node->tag ctx node)
                nil)))
    :map (map-node->tag ctx node)
    :vector (vector-node->tag ctx node)
    nil))

(defn- set-var-type! [ctx ns-sym var-sym t]
  (swap! (:namespaces ctx) update-in
         [(:base-lang ctx) (:lang ctx) ns-sym :vars var-sym]
         (fn [v] (when v (assoc v :type t)))))

(defn reg-defschema-type! [ctx expr]
  (let [children (:children expr)
        var-name (:value (second children))
        schema-node (when (> (count children) 2) (last children))]
    (when (and var-name schema-node)
      (when-let [t (schema-node->tag ctx schema-node)]
        (set-var-type! ctx (-> ctx :ns :name) var-name t)))))

(defn- tag-binding [n tag]
  (if tag
    (with-meta n (assoc (meta n) :tag tag))
    n))

(defn- has-schema-node? [n]
  (and (some? n)
       ;; perf: don't call sexpr if we don't need to
       (identical? :token (utils/tag n))
       (identical? :- (:k n))))

(defn remove-schemas-from-children [ctx expr]
  (let [children (:children expr)
        {:keys [new-children schemas]}
        (loop [[fst-child & rest-children] children
               res {:new-children []
                    :schemas []}]
          (let [expr fst-child]
            (cond (not fst-child)
                  res
                  (= ':- (:k expr))
                  (let [schema-node (first rest-children)
                        remaining (next rest-children)
                        t (schema-node->tag ctx schema-node)]
                    (if (and t (seq remaining) (= :vector (utils/tag (first remaining))))
                      (recur (cons (tag-binding (first remaining) t) (next remaining))
                             (update res :schemas conj schema-node))
                      (recur remaining
                             (update res :schemas conj schema-node))))
                  (= :vector (utils/tag expr))
                  (recur rest-children
                         (let [{:keys [expr schemas]} (remove-schemas-from-children ctx fst-child)]
                           (-> res
                               (update :schemas into schemas)
                               (update :new-children conj expr))))
                  (and (identical? :token (utils/tag expr))
                       (has-schema-node? (first rest-children))
                       (seq (next rest-children)))
                  (let [schema-node (second rest-children)]
                    (recur (nnext rest-children)
                           (-> res
                               (update :schemas conj schema-node)
                               (update :new-children conj
                                       (tag-binding fst-child (schema-node->tag ctx schema-node))))))
                  :else (recur rest-children
                               (update res :new-children conj fst-child)))))]
    {:expr (assoc expr :children new-children)
     :schemas schemas}))

(defn- defmethod-dispatch-val? [fn-sym index]
  (and (= 'defmethod fn-sym) (= 2 index)))

(defn reg-misplaced-return-schema!
  [ctx expr msg]
  (findings/reg-finding!
    ctx
    (utils/node->line (:filename ctx)
                      expr
                      :schema-misplaced-return
                      msg)))

(defn expand-schema
  [ctx fn-sym expr]
  (let [children (:children expr)
        {:keys [new-children
                schemas]}
        (loop [[fst-child & rest-children] children
               index 0
               res {:new-children []
                    :schemas []}
               past-arg-schemas false
               return-tag nil]
          (let [expr fst-child]
            (cond
              (not fst-child)
              res
              ;; Handle defprotocol case - simpler logic that only processes :list nodes
              (and (= 'defprotocol fn-sym)
                   (hooks/list-node? expr))
              (let [new-child (remove-schemas-from-children ctx expr)]
                (recur rest-children
                       index
                       (-> res
                           (update :new-children conj (:expr new-child))
                           (update :schemas into (:schemas new-child)))
                       past-arg-schemas
                       return-tag))
              ;; Handle defprotocol case - other nodes just get added as-is
              (and (= 'defprotocol fn-sym)
                   (not (hooks/list-node? expr)))
              (recur rest-children
                     index
                     (update res :new-children conj expr)
                     past-arg-schemas
                     return-tag)
              ;; Original logic for non-defprotocol cases
              past-arg-schemas
              (if (and (= 'defrecord fn-sym) (hooks/map-node? expr))
                (-> res
                    (update :new-children (fn [children]
                                            (into children rest-children)))
                    (update :schemas conj fst-child))
                (update res :new-children (fn [children]
                                            (into (conj children fst-child) rest-children))))
              (has-schema-node? expr)
              (let [schema-node (first rest-children)]
                (recur (next rest-children)
                       (inc index)
                       (update res :schemas conj schema-node)
                       past-arg-schemas
                       (or (schema-node->tag ctx schema-node) return-tag)))
              (and (hooks/vector-node? expr) (not (defmethod-dispatch-val? fn-sym index)))
              (do ;; detect misplaced return like (s/defn f [] :- Return body)
                (when (and (next rest-children) ;; `(s/defn f [] :-)` is fine
                           (has-schema-node? (first rest-children)))
                  (reg-misplaced-return-schema!
                   ctx (nth children (+ index 2))
                   "Return schema should go before vector."))
                (let [{:keys [expr schemas]} (remove-schemas-from-children ctx fst-child)
                      expr (tag-binding expr return-tag)]
                  (recur rest-children
                         (inc index)
                         (-> res
                             (update :schemas into schemas)
                             (update :new-children conj expr))
                         true
                         return-tag)))
              (and (hooks/list-node? expr)
                   ;; (s/defn foo ()) will fail when expanded form is checked
                   (seq (:children fst-child)))
              (let [res (let [[params & after-params] (:children fst-child)
                              valid-params-position? (= :vector (utils/tag params))
                              ;; detect misplaced return like (s/defn f ([] :- Return body))
                              _ (when (and valid-params-position? ;; (:- Foo []) will be treated as missing params
                                           (next after-params) ;; (s/defn f ([] :-)) is fine
                                           (has-schema-node? (first after-params)))
                                  (reg-misplaced-return-schema!
                                   ctx (second after-params)
                                   "Return schema should go before arities."))
                              {:keys [expr schemas]} (if valid-params-position?
                                                       (remove-schemas-from-children ctx params)
                                                       ;; (s/defn foo (:- Foo [])) expanded forms will warn missing params
                                                       {:expr params
                                                        :schemas []})
                              expr (tag-binding expr return-tag)
                              new-cchildren (cons expr after-params)
                              new-fst-child (assoc fst-child :children new-cchildren)]
                          (-> res
                              (update :schemas into schemas)
                              (update :new-children conj new-fst-child)))]
                (recur rest-children
                       (inc index)
                       res
                       past-arg-schemas
                       return-tag))
              :else (recur rest-children
                           (inc index)
                           (update res :new-children conj fst-child)
                           past-arg-schemas
                           return-tag))))]
    {:expr (assoc expr :children new-children)
     :schemas schemas}))

;;;; Scratch

#_
(comment
  (require '[schema.core :as s])
  (s/defn foo [])
  (s/defn bar [] :-)
  (s/defn baz [] :- s/Int)
  ;; not detected by clj-kondo but immediately fails expansion when evaluated
  (s/defn :- s/Int foo [])
  (s/defn foo ([] :- s/Int))
  ;; not detected by clj-kondo but immediately fails expansion when evaluated
  (s/defn bad-return5
    "foo"
    :- s/Int 
    []
    1))
