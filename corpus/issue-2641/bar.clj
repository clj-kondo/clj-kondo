(ns bar
  (:require [foo]))

(def baz (foo/foo 1 2 3)) ;; should report invalid arity
