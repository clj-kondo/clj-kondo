(ns bar
  (:require [foo :as foo]))

(def baz (foo/foo 1 2 3)) ;; should report invalid arity
