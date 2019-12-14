(ns nested-syntax-quote
  (:require [lib.foo :as foo]))

(defn foo [x]
  `(+ ~(+ 1 2 `~x)))

(defn nested-macro [tag]
  `(defmacro ~(symbol tag) [& args#]
     `(foo/bar ~@args#)))
