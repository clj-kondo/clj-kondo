(ns clj-kondo.impl.metadata
  (:require [rewrite-clj.node.protocols :as node]
            [rewrite-clj.zip :as z]
            [clj-kondo.impl.linters.keys :as key-linter]))

(defn meta? [node]
  (contains? '#{:meta :meta*} (node/tag node)))

(defn lift-meta-content [filename meta-node]
  (let [children (:children meta-node)
        meta-expr (first children)
        meta-val (node/sexpr meta-expr)
        meta-map (cond (keyword? meta-val) {meta-val true}
                       (map? meta-val)
                       (do (key-linter/lint-map-keys filename meta-expr)
                           meta-val)
                       :else {:tag meta-val})
        meta-child (second children)
        meta-child (with-meta meta-child (merge
                                          (meta meta-node)
                                          meta-map
                                          (meta meta-child)))]
    (if (meta? meta-child)
      (recur filename meta-child)
      meta-child)))

(defn lift-meta* [filename zloc]
  (loop [z zloc]
    (let [node (z/node z)
          last? (z/end? z)
          replaced (if (meta? node)
                     (z/replace z
                                (lift-meta-content filename node))
                     z)]
      (if last? replaced
          (recur (z/next replaced))))))

(defn lift-meta [filename expr]
  "Lifts metadata expressions to proper metadata."
  (let [zloc (z/edn* expr)]
    (z/root (lift-meta* filename zloc))))
