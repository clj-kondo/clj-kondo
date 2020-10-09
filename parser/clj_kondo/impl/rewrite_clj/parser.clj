(ns ^{:no-doc true} clj-kondo.impl.rewrite-clj.parser
  (:require [clj-kondo.impl.rewrite-clj.node :as node]
            [clj-kondo.impl.rewrite-clj.parser.core :as p]
            [clj-kondo.impl.rewrite-clj.reader :as reader]))

;; ## Parser Core

(defn parse
  "Parse next form from the given reader."
  [reader]
  (p/parse-next reader))

(defn parse-all
  "Parse all forms from the given reader."
  [reader]
  (let [nodes (->> (repeatedly #(parse reader))
                   (take-while identity)
                   (doall))]
    (with-meta
      (node/forms-node nodes)
      (meta (first nodes)))))

;; ## Specialized Parsers

(defn parse-string
  "Parse first form in the given string."
  [s]
  (parse (reader/string-reader s)))

(defn parse-string-all
  "Parse all forms in the given string."
  [s]
  (parse-all (reader/string-reader s)))

(defn parse-file
  "Parse first form from the given file."
  [f]
  (let [r (reader/file-reader f)]
    (with-open [_ ^java.io.Closeable (.-rdr r)]
      (parse r))))

(defn parse-file-all
  "Parse all forms from the given file."
  [f]
  (let [r (reader/file-reader f)]
    (with-open [_ ^java.io.Closeable (.-rdr r)]
      (parse-all r))))
