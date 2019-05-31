(ns clj-kondo.impl.metadata
  {:no-doc true}
  (:require
   [clj-kondo.impl.linters.keys :as key-linter]
   [clj-kondo.impl.namespace :as namespace]
   [clj-kondo.impl.profiler :as profiler]
   [clj-kondo.impl.utils :as utils]
   [rewrite-clj.node.protocols :as node]))

(defn meta? [node]
  (utils/one-of (node/tag node) [:meta :meta*]))

(defn lift-meta-content [ctx meta-node]
  (if (meta? meta-node)
    (let [children (:children meta-node)
          meta-expr (first children)
          _ (namespace/analyze-usages ctx meta-expr)
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

;;;; Scratch

(comment

  )
