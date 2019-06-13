(ns clj-kondo.impl.schema
  {:no-doc true}
  (:require
   [rewrite-clj.node.protocols :as node]))

(defn remove-schemas-from-children [expr]
  (let [children (:children expr)
        {:keys [:new-children :schemas]}
        (loop [[fst-child & rest-children] children
               res {:new-children []
                    :schemas []}]
          (let [sexpr (when fst-child (node/sexpr fst-child))]
            (cond (not fst-child)
                  res
                  (= ':- (node/sexpr fst-child))
                  (recur (next rest-children)
                         (update res :schemas conj (first rest-children)))
                  (vector? sexpr)
                  (recur rest-children
                         (let [{:keys [:expr :schemas]} (remove-schemas-from-children fst-child)]
                           (-> res
                               (update :schemas into schemas)
                               (update :new-children conj expr))))
                  :else (recur rest-children
                               (update res :new-children conj fst-child)))))]
    {:expr (assoc expr :children new-children)
     :schemas schemas}))

(defn expand-schema-defn2
  [expr]
  (let [children (:children expr)
        {:keys [:new-children
                :schemas]}
        (loop [[fst-child & rest-children] children
               res {:new-children []
                    :schemas []}]
          (let [sexpr (when fst-child (node/sexpr fst-child))]
            (cond (not fst-child)
                  res
                  (= ':- sexpr)
                  (recur (next rest-children)
                         (update res :schemas conj (first rest-children)))
                  (vector? sexpr)
                  (let [{:keys [:expr :schemas]} (remove-schemas-from-children fst-child)]
                    (-> res
                        (update :schemas into schemas)
                        (update :new-children conj expr)
                        (update :new-children into rest-children)))
                  (list? sexpr)
                  (recur rest-children
                         (let [cchildren (:children fst-child)
                               {:keys [:expr :schemas]} (remove-schemas-from-children (first cchildren))
                               new-cchildren (cons expr (rest cchildren))
                               new-fst-child (assoc fst-child :children new-cchildren)]
                           (-> res
                               (update :schemas into schemas)
                               (update :new-children conj new-fst-child))))
                  :else (recur rest-children
                               (update res :new-children conj fst-child)))))]
    {:defn (assoc expr :children new-children)
     :schemas schemas}))

;;;; Scratch

(comment 
  )
