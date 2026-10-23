(ns ^{:no-doc true} clj-kondo.impl.rewrite-clj.node.token
  (:require [clj-kondo.impl.rewrite-clj.node.protocols :as node]
            [clojure.string :as str]))

(set! *warn-on-reflection* true)

(def ^:dynamic *syntax-quote-imports*
  "When bound to a map of class name to package, QuoteNode's `string` method
  binds `*imports*` to it inside a syntax-quote. Used by gen-macros."
  nil)

(def ^:dynamic *imports*
  "When bound to a map of class name to package, TokenNode's `string` method
  emits an imported class name fully qualified."
  nil)

(defn- qualified-class-string [imports sym]
  (when (symbol? sym)
    (let [ns (namespace sym)
          n (name sym)]
      (cond
        ns (when-let [pkg (get imports (symbol ns))]
             (str pkg "." ns "/" n))
        (str/ends-with? n ".")
        (when-let [pkg (get imports (symbol (subs n 0 (dec (count n)))))]
          (str pkg "." n))
        :else (when-let [pkg (get imports sym)]
                (str pkg "." n))))))

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
                  (if-some [mta (meta this)]
                    (with-meta value
                               (reduce #(conj %1 (->meta (node/sexpr %2))) mta (:meta this)))
                    value)
                  value))
  (length [_] (count string-value))
  (string [_]
    (if-let [imports *imports*]
      (or (qualified-class-string imports value) string-value)
      string-value))

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! TokenNode)

;; ## Constructor

(defn token-node
  "Create node for an unspecified EDN token."
  ([value]
   (token-node value nil))
  ([value string-value]
   (->TokenNode value (or string-value (pr-str value)))))
