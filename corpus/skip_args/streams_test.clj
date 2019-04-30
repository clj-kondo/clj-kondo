(ns skip-args.streams-test
  (:require [riemann.test :refer [test-stream]]))

(test-stream (select-keys {:a 1 :a 2}))
