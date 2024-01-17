(ns ^{:no-doc true} clj-kondo.impl.rewrite-clj.node.token
  (:require [clj-kondo.impl.rewrite-clj.node.protocols :as node]))

(set! *warn-on-reflection* true)

;; ## Node

(defn ->meta [x]
  (cond (map? x) x
        (symbol? x) {:tag x}
        (keyword? x) {x true}
        :else {x true}))

(defrecord TokenNode [value string-value]
  node/Node
  (tag [_] :token)
  (printable-only? [_] false)
  (sexpr [this] (if (instance? clojure.lang.IObj value)
                  (with-meta value (reduce merge (meta this)
                                           (map (comp ->meta node/sexpr)
                                                (:meta this))))
                  value))
  (length [_] (count string-value))
  (string [_] string-value)

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! TokenNode)

;; ## Constructor

(defn token-node
  "Create node for an unspecified EDN token."
  [value & [string-value]]
  (->TokenNode
    value
    (or string-value (pr-str value))))
