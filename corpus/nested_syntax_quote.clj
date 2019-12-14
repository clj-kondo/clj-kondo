(ns nested-syntax-quote
  (:require [lib.foo :as foo]))

(defn nested-macro [tag]
  `(defmacro ~(symbol tag) [& args#]
     `(foo/bar ~@args#)))
