(ns clj-kondo.impl.schema
  {:no-doc true}
  (:require
   [clj-kondo.hooks-api :as hooks]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.schema-types :as schema-types]
   [clj-kondo.impl.utils :as utils]))

(defn remove-schemas-from-children [expr]
  (let [children (:children expr)
        {:keys [:new-children :schemas]}
        (loop [[fst-child & rest-children] children
               res {:new-children []
                    :schemas []}]
          (let [expr fst-child]
            (cond (not fst-child)
                  res
                  (= ':- (:k expr))
                  (recur (next rest-children)
                         (update res :schemas conj (first rest-children)))
                  (= :vector (utils/tag expr))
                  (recur rest-children
                         (let [{:keys [:expr :schemas]} (remove-schemas-from-children fst-child)]
                           (-> res
                               (update :schemas into schemas)
                               (update :new-children conj expr))))
                  :else (recur rest-children
                               (update res :new-children conj fst-child)))))]
    {:expr (assoc expr :children new-children)
     :schemas schemas}))

(defn- has-schema-node? [n]
  (and (some? n)
       ;; perf: don't call sexpr if we don't need to
       (identical? :token (utils/tag n))
       (identical? :- (:k n))))

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

(defn store-function-schema-info!
  "Store schema type information in context for integration with clj-kondo's type system"
  [ctx fn-name schemas]
  (when (and fn-name (seq schemas))
    (let [schema-types (mapv schema-types/extract-schema-type schemas)
          type-spec (schema-types/convert-schema-to-type-spec schema-types)]
      (when type-spec
        ;; Store in context so analyze-defn can access it
        (swap! (:schema-arities ctx (atom {})) 
               assoc fn-name (:arities type-spec))))))

(defn get-schema-type-info
  "Retrieve stored schema type information"
  [ctx ns-name fn-name]
  (get-in @(:schema-type-info ctx (atom {})) [ns-name fn-name]))

(defn expand-schema
  [ctx fn-sym expr]
  (let [children (:children expr)
        {:keys [new-children
                schemas]}
        (loop [[fst-child & rest-children] children
               index 0
               res {:new-children []
                    :schemas []}
               past-arg-schemas false]
          (let [expr fst-child]
            (cond
              (not fst-child)
              res
              ;; Handle defprotocol case - simpler logic that only processes :list nodes
              (and (= 'defprotocol fn-sym)
                   (hooks/list-node? expr))
              (let [new-child (remove-schemas-from-children expr)]
                (recur rest-children
                       index
                       (-> res
                           (update :new-children conj (:expr new-child))
                           (update :schemas into (:schemas new-child)))
                       past-arg-schemas))
              ;; Handle defprotocol case - other nodes just get added as-is
              (and (= 'defprotocol fn-sym)
                   (not (hooks/list-node? expr)))
              (recur rest-children
                     index
                     (update res :new-children conj expr)
                     past-arg-schemas)
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
              (recur (next rest-children)
                     (inc index)
                     (update res :schemas conj (first rest-children))
                     past-arg-schemas)
              (and (hooks/vector-node? expr) (not (defmethod-dispatch-val? fn-sym index)))
              (do ;; detect misplaced return like (s/defn f [] :- Return body)
                (when (and (next rest-children) ;; `(s/defn f [] :-)` is fine
                           (has-schema-node? (first rest-children)))
                  (reg-misplaced-return-schema!
                   ctx (nth children (+ index 2))
                   "Return schema should go before vector."))
                (let [{:keys [expr schemas]} (remove-schemas-from-children fst-child)]
                  (recur rest-children
                         (inc index)
                         (-> res
                             (update :schemas into schemas)
                             (update :new-children conj expr))
                         true)))
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
                              {:keys [:expr :schemas]} (if valid-params-position?
                                                         (remove-schemas-from-children params)
                                                         ;; (s/defn foo (:- Foo [])) expanded forms will warn missing params
                                                         {:expr params
                                                          :schemas []})
                              new-cchildren (cons expr after-params)
                              new-fst-child (assoc fst-child :children new-cchildren)]
                          (-> res
                              (update :schemas into schemas)
                              (update :new-children conj new-fst-child)))]
                (recur rest-children
                       (inc index)
                       res
                       past-arg-schemas))
              :else (recur rest-children
                           (inc index)
                           (update res :new-children conj fst-child)
                           past-arg-schemas))))]
    (when (and (seq schemas)
               (#{'defn 'defn-} fn-sym))
      (let [fn-name (some-> expr :children second :value)]
        (when fn-name
          (store-function-schema-info! ctx fn-name schemas))))
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
