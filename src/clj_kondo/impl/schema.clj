(ns clj-kondo.impl.schema
  {:no-doc true}
  (:require
   [clj-kondo.hooks-api :as hooks]
   [clj-kondo.impl.findings :as findings]
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
       (= :keyword (utils/tag n))
       (= :- (utils/sexpr n))))

(defn- defmethod-dispatch-val? [fn-sym index]
  (and (= 'defmethod fn-sym) (= 2 index)))

(defn- reg-suspicious-return-schema!
  [ctx expr]
  (prn "reg-finding!" expr)
  (findings/reg-finding!
    ctx
    (-> (utils/node->line (:filename ctx)
                          expr
                          :suspicious-schema-return
                          "Return schema should go before vector.")
        (assoc :level :warning))))

(defn expand-schema
  [ctx fn-sym expr]
  (let [children (:children expr)
        nchildren (count children)
        {:keys [new-children
                schemas]}
        (loop [[fst-child & rest-children] children
               index 0
               res {:new-children []
                    :schemas []}
               past-arg-schemas false]
          (let [expr fst-child]
            (cond
              past-arg-schemas
              (if (and (= 'defrecord fn-sym)
                       (hooks/map-node? expr))
                (-> res
                    (update :new-children (fn [children]
                                            (into children rest-children)))
                    (update :schemas conj fst-child))
                (update res :new-children (fn [children]
                                            (into (conj children fst-child) rest-children))))
              (not fst-child)
              res
              (= ':- (:k expr))
              (recur (next rest-children)
                     (inc index)
                     (update res :schemas conj (first rest-children))
                     past-arg-schemas)
              (and (hooks/vector-node? expr) (not (defmethod-dispatch-val? fn-sym index)))
              (let [_ (when (and (< (inc index) nchildren) ;; `(s/defn f [] :-)` is fine
                                 (has-schema-node? (nth children (inc index))))
                        (reg-suspicious-return-schema! ctx (nth children (inc index))))
                    {:keys [expr schemas]} (remove-schemas-from-children fst-child)]
                (recur rest-children
                       (inc index)
                       (-> res
                           (update :schemas into schemas)
                           (update :new-children conj expr))
                       true))
              (and (hooks/list-node? expr)
                   ;; (s/defn foo ()) will fail when expanded form is checked
                   (seq (:children fst-child)))
              (recur rest-children
                     (inc index)
                     (let [[params & after-params] (:children fst-child)
                           valid-params-position? (= :vector (utils/tag params))
                           _ (when (and (not valid-params-position?) ;; (:- Foo []) will be treated as missing params
                                        (next after-params) ;; ([] :-) is fine
                                        (has-schema-node? (first after-params)))
                               (reg-suspicious-return-schema! ctx (first after-params)))
                           {:keys [:expr :schemas]} (if valid-params-position?
                                                      (remove-schemas-from-children params)
                                                      ;; (s/defn foo (:- Foo [])) expanded forms will warn missing params
                                                      {:expr params
                                                       :schemas []})
                           new-cchildren (cons expr after-params)
                           new-fst-child (assoc fst-child :children new-cchildren)]
                       (-> res
                           (update :schemas into schemas)
                           (update :new-children conj new-fst-child)))
                     past-arg-schemas)
              :else (recur rest-children
                           (inc index)
                           (update res :new-children conj fst-child)
                           past-arg-schemas))))]
    {:expr (assoc expr :children new-children)
     :schemas schemas}))

;;;; Scratch

(comment)
