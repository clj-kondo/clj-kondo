(ns ^{:no-doc true} clj-kondo.impl.rewrite-clj.node.whitespace
  (:require [clj-kondo.impl.rewrite-clj.node.protocols :as node]
            [clj-kondo.impl.rewrite-clj.reader :as r]))

;; ## Newline Modifiers

(def ^:dynamic *newline-fn*
  "This function is applied to every newline string."
  identity)

(def ^:dynamic *count-fn*
  "This function is applied to every newline string and should produce
   the eventual character count."
  count)

(defmacro with-newline-fn
  [f & body]
  `(binding [*newline-fn* (comp *newline-fn* ~f)]
     ~@body))

(defmacro with-count-fn
  [f & body]
  `(binding [*count-fn* (comp *count-fn* ~f)]
     ~@body))

;; ## Nodes

(defrecord WhitespaceNode [whitespace]
  node/Node
  (tag [_] :whitespace)
  (printable-only? [_] true)
  (sexpr [_] (throw (UnsupportedOperationException.)))
  (length [_] (count whitespace))
  (string [_] whitespace)

  Object
  (toString [this]
    (node/string this)))

(defrecord CommaNode [commas]
  node/Node
  (tag [_] :comma)
  (printable-only? [_] true)
  (sexpr [_] (throw (UnsupportedOperationException.)))
  (length [_] (count commas))
  (string [_] commas)

  Object
  (toString [this]
    (node/string this)))

(defrecord NewlineNode [newlines]
  node/Node
  (tag [_] :newline)
  (printable-only? [_] true)
  (sexpr [_] (throw (UnsupportedOperationException.)))
  (length [_] (*count-fn* newlines))
  (string [_] (*newline-fn* newlines))

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! WhitespaceNode)
(node/make-printable! CommaNode)
(node/make-printable! NewlineNode)

;; ## Constructors

(defn- string-of?
  [^String s pred]
  (and s
       (string? s)
       (pos? (.length s))
       (every? pred s)))

(defn whitespace-node
  "Create whitespace node."
  [s]
  {:pre [(string-of? s r/space?)]}
  (->WhitespaceNode s))

(defn comma-node
  "Create comma node."
  [s]
  {:pre [(string-of? s r/comma?)]}
  (->CommaNode s))

(defn newline-node
  "Create newline node."
  [s]
  {:pre [(string-of? s r/linebreak?)]}
  (->NewlineNode s))

(defn- classify-whitespace
  [c]
  (cond (r/comma? c)     :comma
        (r/linebreak? c) :newline
        :else :whitespace))

(defn whitespace-nodes
  "Convert a string of whitespace to whitespace/newline nodes."
  [s]
  {:pre [(string-of? s r/whitespace?)]}
  (->> (partition-by classify-whitespace s)
       (map
         (fn [char-seq]
           (let [s (apply str char-seq)]
             (case (classify-whitespace (first char-seq))
               :comma   (comma-node s)
               :newline (newline-node s)
               (whitespace-node s)))))))

;; ## Utilities

(defn spaces
  "Create node representing the given number of spaces."
  [n]
  (whitespace-node (apply str (repeat n \space))))

(defn newlines
  "Create node representing the given number of newline characters."
  [n]
  (newline-node (apply str (repeat n \newline))))

(let [comma (whitespace-nodes ", ")]
  (defn comma-separated
    "Interleave the given seq of nodes with `\", \"` nodes."
    [nodes]
    (->> nodes
         (mapcat #(cons % comma))
         (drop-last (count comma)))))

(let [nl (newline-node "\n")]
  (defn line-separated
    "Interleave the given seq of nodes with newline nodes."
    [nodes]
    (butlast (interleave nodes (repeat nl)))))

(let [space (whitespace-node " ")]
  (defn space-separated
    "Interleave the given seq of nodes with `\" \"` nodes."
    [nodes]
    (butlast (interleave nodes (repeat space)))))

;; ## Predicates

(defn whitespace?
  "Check whether a node represents whitespace."
  [node]
  (contains?
   #{:whitespace
     :newline
     :comma}
   (node/tag node)))

(defn linebreak?
  "Check whether a ndoe represents linebreaks."
  [node]
  (= (node/tag node) :newline))

(defn comma?
  "Check whether a node represents a comma."
  [node]
  (= (node/tag node) :comma))
