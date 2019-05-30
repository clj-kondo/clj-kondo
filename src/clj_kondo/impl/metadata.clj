(ns clj-kondo.impl.metadata
  {:no-doc true}
  (:require
   [clj-kondo.impl.linters.keys :as key-linter]
   [clj-kondo.impl.profiler :as profiler]
   [clj-kondo.impl.utils :as utils]
   [rewrite-clj.node.protocols :as node]
   [clj-kondo.impl.namespace :as namespace]))

(defn meta? [node]
  (utils/one-of (node/tag node) [:meta :meta*]))

(defn lift-meta-content [ctx meta-node]
  (if (meta? meta-node)
    (let [children (:children meta-node)
          meta-expr (first children)
          _ (namespace/used-namespaces ctx meta-expr)
          meta-val (node/sexpr meta-expr)
          meta-map (cond (keyword? meta-val) {meta-val true}
                         (map? meta-val)
                         (do (key-linter/lint-map-keys ctx meta-expr)
                             meta-val)
                         :else {:tag meta-val})
          meta-child (second children)
          meta-child (with-meta meta-child (merge
                                            (meta meta-node)
                                            meta-map
                                            (meta meta-child)))]
      (if (meta? meta-child)
        (recur ctx meta-child)
        meta-child))
    meta-node))

(declare lift-meta*)

(defn lift-meta-children [ctx expr]
  (if-let [children (:children expr)]
    (let [new-children (doall (map #(lift-meta* ctx %) children))]
      (assoc expr :children new-children))
    expr))

(defn lift-meta* [ctx expr]
  (lift-meta-children ctx (lift-meta-content ctx expr)))

(defn lift-meta [ctx expr]
  (profiler/profile
   :lift-meta
   (lift-meta* ctx expr)))

;;;; Scratch

(comment
  (lift-meta "." (parse-string "^{:a 1} [1 2 3]"))

  )
