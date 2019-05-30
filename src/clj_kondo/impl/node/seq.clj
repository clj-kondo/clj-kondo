(ns clj-kondo.impl.node.seq
  {:no-doc true}
  (:require [rewrite-clj.node.protocols :as node]))

(set! *warn-on-reflection* true)

(defrecord NamespacedMapNode [ns aliased? children]
  node/Node
  (tag [this]
    :namespaced-map)
  (printable-only? [_] false)
  (sexpr [this]
    (let [nspace-k (:k ns)
          m (first (node/sexprs children))
          nspace (name nspace-k)]
      (->> (for [[k v] m
                 :let [k' (cond (not (keyword? k)) k
                                (namespace k)      k
                                :else (keyword nspace (name k)))]]
             [k' v])
           (into {}))))
  (length [_]
    (+ 1 (node/sum-lengths children)))
  (string [this]
    (str "#" ns (node/concat-strings children)))

  node/InnerNode
  (inner? [_] true)
  (children [_] children)
  (replace-children [this children']
    (assoc this :children children'))
  (leader-length [_]
    1)

  Object
  (toString [this]
    (node/string this)))

(defn namespaced-map-node [map-ns aliased? children]
  (NamespacedMapNode. map-ns aliased? children))

;;;; Scratch

(comment

  )
