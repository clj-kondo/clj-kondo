(ns clj-kondo.impl.metadata
  {:no-doc true}
  (:require
   [clj-kondo.impl.linters.keys :as key-linter]
   [clj-kondo.impl.profiler :as profiler]
   [rewrite-clj.node.protocols :as node]))

(defn meta? [node]
  (contains? '#{:meta :meta*} (node/tag node)))

(defn lift-meta-content [filename meta-node]
  (if (meta? meta-node)
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
        meta-child))
    meta-node))

#_(defn lift-meta* [filename zloc]
    (loop [z zloc]
      (let [node (z/node z)
            last? (z/end? z)
            replaced (if (meta? node)
                       (z/replace z
                                  (lift-meta-content filename node))
                       z)]
        (if last? replaced
            (recur (z/next replaced))))))

#_(defn lift-meta [filename expr]
    "Lifts metadata expressions to proper metadata."
    (profiler/profile
     :lift-meta
     (let [zloc (z/edn* expr)]
       (z/root (lift-meta* filename zloc)))))

;; the above zipper implementation is much slower

(declare lift-meta*)

(defn lift-meta-children [filename expr]
  (if-let [children (:children expr)]
    (let [new-children (doall (map #(lift-meta* filename %) children))]
      (assoc expr :children new-children))
    expr))

(defn lift-meta* [filename expr]
  (lift-meta-children filename (lift-meta-content filename expr)))

(defn lift-meta [filename expr]
  (profiler/profile
   :lift-meta
   (lift-meta* filename expr)))

;;;; Scratch

(comment
  (lift-meta "." (parse-string "^{:a 1} [1 2 3]"))

  )
