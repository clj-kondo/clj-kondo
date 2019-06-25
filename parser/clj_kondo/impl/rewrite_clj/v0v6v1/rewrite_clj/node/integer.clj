(ns ^{:no-doc true} clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.node.integer
  (:require [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.node.protocols :as node]))

;; ## Node

(defrecord IntNode [value base]
  node/Node
  (tag [_] :token)
  (printable-only? [_] false)
  (sexpr [_] value)
  (length [this]
    (count (node/string this)))
  (string [_]
    (let [sign (if (< value 0)
                 "-")
          abs-value (cond-> value (< value 0) -)
          s (.toString (biginteger abs-value) base)
          prefix (case (long base)
                   8  "0"
                   10 ""
                   16 "0x"
                   (str base "r"))]
      (str sign prefix s)))

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! IntNode)

;; ## Constructor

(defn integer-node
  "Create node for an EDN integer with the given base."
  ([value]
   (integer-node value 10))
  ([value base]
   {:pre [(integer? value)
          (integer? base)
          (< 1 base 37)]}
   (->IntNode value base)))
