(ns ^{:no-doc true} clj-kondo.impl.rewrite-clj.parser.string
  (:require
   [clj-kondo.impl.rewrite-clj.node :as node]
   [clj-kondo.impl.rewrite-clj.parser
             [utils :as u]]
   [clojure.string :as string]))

(defn parse-string
  [reader]
  (node/string-node (u/read-string-data reader)))

(defn parse-regex
  [reader]
  (let [h (u/read-string-data reader)]
    (string/join "\n" h)))
