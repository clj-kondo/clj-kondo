(ns ^{:no-doc true} clj-kondo.impl.rewrite-clj.node.seq
  (:require [clj-kondo.impl.rewrite-clj.node.protocols :as node]))

;; ## Nodes

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
    (seq-fn (node/sexprs children)))
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

(defrecord NamespacedMapNode [children]
  node/Node
  (tag [this]
    :namespaced-map)
  (printable-only? [_] false)
  (sexpr [this]
    (let [[nspace' m] (node/sexprs children)
          nspace (if (namespace nspace')
                   (-> (ns-aliases *ns*)
                       (get (symbol (name nspace')))
                       (ns-name)
                       (name))
                   (name nspace'))]
      (assert nspace
              (str "could not resolve namespace alias for namespaced map: "
                   (namespace nspace')))
      (->> (for [[k v] m
                 :let [k' (cond (not (keyword? k)) k
                                (namespace k)      k
                                :else (keyword nspace (name k)))]]
             [k' v])
           (into {}))))
  (length [_]
    (+ 1 (node/sum-lengths children)))
  (string [this]
    (str "#" (node/concat-strings children)))

  node/InnerNode
  (inner? [_] true)
  (children [_] children)
  (replace-children [this children']
    (assert-namespaced-map-children children')
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

(defn namespaced-map-node
  "Create a node representing an EDN map namespace."
  [children]
  (assert-namespaced-map-children children)
  (->NamespacedMapNode children))
