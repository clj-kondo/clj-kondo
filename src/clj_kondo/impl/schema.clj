(ns clj-kondo.impl.schema
  {:no-doc true}
  (:require
   [clj-kondo.hooks-api :as hooks]
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

(defn expand-schema
  [_ctx fn-sym expr]
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
              (let [suspicious-return-schema?
                    (and (< (inc index) nchildren) ;; `(s/defn f [] :-)` is fine
                         (has-schema-node? (nth children (inc index))))
                    {:keys [expr schemas]} (remove-schemas-from-children fst-child)]
                (recur rest-children
                       (inc index)
                       (-> res
                           (update :schemas into schemas)
                           (update :new-children conj expr))
                       true))
              (hooks/list-node? expr)
              (recur rest-children
                     (inc index)
                     (let [[params & after-params] (:children fst-child)
                           valid-params-position? (= :vector (some-> params utils/tag))
                           suspicious-return-schema? (and (not valid-params-position?)
                                                          (next after-params) ;; ([] :-) is fine
                                                          (has-schema-node? (first after-params)))
                           {:keys [:expr :schemas]} (if true #_valid-params-position?
                                                      (remove-schemas-from-children params)
                                                      (do
                                                        (prn "TODO warn")
                                                        {:expr params
                                                         :schemas []}))
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
