(ns my-test2
  (:require [foo :as foo]))

(defn my-test [& xs]
  (prn xs))

(defmacro my-test-macro [x]
  x)

(my-test
 (with-redefs [inc dec] 1)
 (foo/with-redefs [inc dec] 1))

(my-test-macro [])
