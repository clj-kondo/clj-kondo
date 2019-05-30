(ns clj-kondo.impl.metadata
  {:no-doc true}
  (:require
   [clj-kondo.impl.linters.keys :as key-linter]
   [clj-kondo.impl.profiler :as profiler]
   [clj-kondo.impl.utils :as utils]
   [rewrite-clj.node.protocols :as node]))

(defn meta? [node]
  (utils/one-of (node/tag node) [:meta :meta*]))

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
