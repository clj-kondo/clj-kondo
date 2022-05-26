(ns ^{:no-doc true} clj-kondo.impl.rewrite-clj.node.seq
  (:require [clj-kondo.impl.rewrite-clj.node.protocols :as node]))

;; ## Nodes

(set! *warn-on-reflection* true)

(defrecord SeqNode [tag
                    format-string
                    wrap-length
                    seq-fn
                    children]
  node/Node
  (tag [this]
    tag)
  (printable-only? [_] false)
  (sexpr [this]
    ;; (prn (meta this))
    (with-meta (seq-fn (node/sexprs children))
      (meta this)))
  (length [_]
    (+ wrap-length (node/sum-lengths children)))
  (string [this]
    (->> (node/concat-strings children)
         (format format-string)))

  node/InnerNode
  (inner? [_]
    true)
  (children [_]
    children)
  (replace-children [this children']
    (assoc this :children children'))
  (leader-length [_]
    (dec wrap-length))

  Object
  (toString [this]
    (node/string this)))

(defn- assert-namespaced-map-children
  [children]
  (let [exs (node/sexprs children)]
    (assert (= (count exs) 2)
            "can only contain 2 non-whitespace forms.")
    (assert (keyword? (first exs))
            "first form in namespaced map needs to be keyword.")
    (assert (map? (second exs))
            "second form in namespaced map needs to be map.")))

(defrecord NamespacedMapNode [tag ns aliased? children]
  node/Node
  (tag [this]
    tag)
  (printable-only? [_] false)
  (sexpr [this]
    (let [nspace-k (:k ns)
          m (first (node/sexprs children))
          nspace (name nspace-k)
          m (->> (for [[k v] m
                       :let [k' (cond (qualified-ident? k) k
                                      (keyword? k) (keyword nspace (name k))
                                      (symbol? k) (symbol nspace (name k))
                                      :else k)]]
                   [k' v])
                 (into {}))]
      m))
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

(node/make-printable! SeqNode)
(node/make-printable! NamespacedMapNode)

;; ## Constructors

(defn list-node
  "Create a node representing an EDN list."
  [children]
  (->SeqNode :list "(%s)" 2 #(apply list %) children))

(defn vector-node
  "Create a node representing an EDN vector."
  [children]
  (->SeqNode :vector "[%s]" 2 vec children))

(defn set-node
  "Create a node representing an EDN set."
  [children]
  (->SeqNode :set "#{%s}" 3 set children))

(defn map-node
  "Create a node representing an EDN map."
  [children]
  (->SeqNode :map "{%s}" 2 #(apply hash-map %) children))

(defn namespaced-map-node [map-ns aliased? children]
  (NamespacedMapNode. :namespaced-map map-ns aliased? children))
