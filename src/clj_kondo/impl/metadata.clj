(ns clj-kondo.impl.metadata
  {:no-doc true}
  (:require
   [clj-kondo.impl.linters.keys :as key-linter]
   [clj-kondo.impl.utils :as utils]
   [clj-kondo.impl.analyzer.usages :refer [analyze-usages2]]))

(defn meta? [node]
  (utils/one-of (utils/tag node) [:meta :meta*]))

(defn lift-meta-content [ctx meta-node]
  (if (meta? meta-node)
    (let [children (:children meta-node)
          meta-expr (first children)
          _ (analyze-usages2 ctx meta-expr)
          meta-val (utils/sexpr meta-expr)
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

(defn meta-node->map [ctx node]
  (let [s (utils/sexpr node)]
    (cond (keyword? s) {s true}
          (map? s)
          (do
            (key-linter/lint-map-keys ctx node)
            s)
          :else {:tag s})))

(defn lift-meta-content2 [ctx node]
  (if-let [meta-list (:meta node)]
    (let [_ (run! #(analyze-usages2 ctx %) meta-list)
          meta-maps (map #(meta-node->map ctx %) meta-list)
          meta-map (apply merge meta-maps)
          node (-> node
                   (dissoc :meta)
                   (with-meta (merge (meta node) meta-map)))]
      node)
    node))

;;;; Scratch

(comment
  (meta (lift-meta-content2 {} (clj-kondo.impl.utils/parse-string "^{:a 1 :a 2} []")))
  )
