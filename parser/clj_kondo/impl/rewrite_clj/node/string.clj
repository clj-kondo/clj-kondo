(ns ^{:no-doc true} clj-kondo.impl.rewrite-clj.node.string
  (:require [clj-kondo.impl.rewrite-clj.node.protocols :as node]
            [clojure.string :as string]
            [edamame.impl.reader.edn :as edn]))

;; ## Node

(defn- wrap-string
  [s]
  (str "\"" s "\""))

(defn- join-lines
  [lines]
  (string/join "\n" lines))

(defn- maybe-unescape
  [s]
  (if (string/includes? s "\\")
    (edn/read-string (wrap-string s))
    s))

(defrecord StringNode [lines]
  node/Node
  (tag [_]
    (if (next lines)
      :multi-line
      :token))
  (printable-only? [_]
    false)
  (sexpr [_]
    (if (= (count lines) 1)
      (maybe-unescape (nth lines 0))
      (join-lines (map maybe-unescape lines))))
  (length [_]
    (+ 2 (reduce + (map count lines))))
  (string [_]
    (wrap-string (join-lines lines)))

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! StringNode)

;; ## Constructors

(defn string-node
  "Create node representing a string value.
   Takes either a seq of strings or a single one."
  [lines]
  (if (string? lines)
    (->StringNode [lines])
    (->StringNode lines)))
