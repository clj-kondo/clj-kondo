(ns clj-kondo.impl.schema
  {:no-doc true}
  (:require
   [clj-kondo.hooks-api :as hooks]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.utils :as utils]))

(defn remove-schemas-from-children [expr]
  (let [children (:children expr)
        {:keys [new-children schemas]}
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
                         (let [{:keys [expr schemas]} (remove-schemas-from-children fst-child)]
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
                              {:keys [expr schemas]} (if valid-params-position?
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
    {:expr (assoc expr :children new-children)
     :schemas schemas}))

;;;; Schema information for analysis output

(defn- arg-vec->inputs
  "Given an arg vector node with schema annotations, returns a vector with one
  map per argument: `{:arg \"x\" :schema \"s/Int\"}`. `:schema` is absent when
  the argument has no schema. The `&` symbol is dropped and the rest argument
  is marked with `:rest true`."
  [arg-vec]
  (loop [[node & more] (:children arg-vec)
         rest? false
         inputs []]
    (cond
      (nil? node) inputs
      (has-schema-node? node)
      (let [schema-node (first more)]
        (recur (next more)
               rest?
               (if (and schema-node (seq inputs))
                 (assoc-in inputs [(dec (count inputs)) :schema] (str schema-node))
                 inputs)))
      (= '& (:value node)) (recur more true inputs)
      :else
      (let [arg (if (= :vector (utils/tag node))
                  ;; destructuring vector: drop inner schemas from the arg string
                  (:expr (remove-schemas-from-children node))
                  node)]
        (recur more
               rest?
               (conj inputs (cond-> {:arg (str arg)}
                              rest? (assoc :rest true))))))))

(defn defn-schema-info
  "Extracts schema information from an `s/defn` form (before schemas are
  removed). Returns `{:output \"Ret\" :arities [{:inputs [...]}]}`, where
  `:output` is absent when there is no return schema. Schemas are returned as
  written in the source: a symbol name for named schemas (e.g. from
  `s/defschema`) or the full inline form."
  [expr]
  (let [[_defn _name & children] (:children expr)
        {:keys [output bodies]}
        (loop [[node & more] children
               output nil]
          (cond
            (nil? node) {:output output}
            (has-schema-node? node) (recur (next more) (or output (first more)))
            (hooks/vector-node? node) {:output output :bodies [node]}
            (hooks/list-node? node) {:output output
                                     :bodies (filter hooks/list-node? (cons node more))}
            ;; docstring, attr-map
            :else (recur more output)))
        arities (into []
                      (keep (fn [body]
                              (let [arg-vec (if (hooks/vector-node? body)
                                              body
                                              (first (:children body)))]
                                (when (hooks/vector-node? arg-vec)
                                  {:inputs (arg-vec->inputs arg-vec)}))))
                      bodies)]
    (cond-> {:arities arities}
      output (assoc :output (str output)))))

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
