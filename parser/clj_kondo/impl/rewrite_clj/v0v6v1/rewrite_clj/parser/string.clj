(ns ^{:no-doc true} clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.parser.string
  (:require [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.parser
             [utils :as u]]
            [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.node :as node]
            [clj-kondo.impl.toolsreader.v1v2v2.clojure.tools.reader
             [edn :as edn]
             [reader-types :as r]]
            [clojure.string :as string]))

(defn- flush-into
  "Flush buffer and add string to the given vector."
  [lines ^StringBuffer buf]
  (let [s (str buf)]
    (.setLength buf 0)
    (conj lines s)))

(defn- read-string-data
  [reader]
  (u/ignore reader)
  (let [buf (StringBuffer.)]
    (loop [escape? false
           lines []]
      (if-let [c (r/read-char reader)]
        (cond (and (not escape?) (= c \"))
              (flush-into lines buf)

              (= c \newline)
              (recur escape? (flush-into lines buf))

              :else
              (do
                (.append buf c)
                (recur (and (not escape?) (= c \\)) lines)))
        (u/throw-reader reader "Unexpected EOF while reading string.")))))

(defn parse-string
  [reader]
  (node/string-node (read-string-data reader)))

(defn parse-regex
  [reader]
  (let [h (read-string-data reader)]
    (string/join "\n" h)))
