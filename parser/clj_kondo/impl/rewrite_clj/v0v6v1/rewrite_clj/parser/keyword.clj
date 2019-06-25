(ns ^{:no-doc true} clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.parser.keyword
  (:require [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.node :as node]
            [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.parser.utils :as u]
            [clj-kondo.impl.toolsreader.v1v2v2.clojure.tools.reader
             [edn :as edn]
             [reader-types :as r]]))

(defn parse-keyword
  [reader]
  (u/ignore reader)
  (if-let [c (r/peek-char reader)]
    (if (= c \:)
      (node/keyword-node
        (edn/read reader)
        true)
      (do
        (r/unread reader \:)
        (node/keyword-node (edn/read reader))))
    (u/throw-reader reader "unexpected EOF while reading keyword.")))
