(ns use-aot-arity-error
  (:require [aot-test.sample :refer [one-arg two-args]]))

(one-arg 1 2 3)   ;; wrong arity - expects 1
(two-args 1)       ;; wrong arity - expects 2
