(ns clj-kondo.impl.parser
  {:no-doc true}
  (:require
   [clj-kondo.impl.rewrite-clj.parser :as p]))

(defn parse-string [s]
  (let [parsed (p/parse-string-all s)]
    parsed))

;;;; Scratch

