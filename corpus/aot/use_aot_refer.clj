(ns use-aot-refer
  (:require [aot-test.sample :refer [one-arg two-args varargs]]))

(one-arg 1)
(two-args 1 2)
(varargs 1 2 3)
