(ns use-aot
  (:require [aot-test.sample :refer :all]))

(one-arg 1)
(two-args 1 2)
(three-args 1 2 3)
(varargs 1 2 3)
(sample-macro (+ 1 2))
a-value
