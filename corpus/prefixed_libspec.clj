(ns foo.bar.baz)

(defn b [_])

(ns foo.baz)

(defn c [_])

(ns prefixed-libspec
  (:require [foo
             [bar [baz :as b]]
             [baz :as c]]))

(b/b) ;; invalid arity
(c/c) ;; invalid arity
