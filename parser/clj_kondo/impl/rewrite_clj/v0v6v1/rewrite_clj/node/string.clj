(ns ^{:no-doc true} clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.node.string
  (:require [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.node.protocols :as node]
            [clj-kondo.impl.toolsreader.v1v2v2.clojure.tools.reader.edn :as edn]
            [clojure.string :as string]))

;; ## Node

(defn- wrap-string
  [s]
  (format "\"%s\"" s))

(defn- join-lines
  [lines]
  (string/join "\n" lines))

(defrecord StringNode [lines]
  node/Node
  (tag [_]
    (if (next lines)
      :multi-line
      :token))
  (printable-only? [_]
    false)
  (sexpr [_]
    (join-lines
      (map
        (comp edn/read-string wrap-string)
        lines)))
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
