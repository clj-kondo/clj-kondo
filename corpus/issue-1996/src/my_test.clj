(ns my-test
  (:require [foo :as foo]))

(defn my-test [& xs]
  (prn xs))

(my-test
 (with-redefs [inc dec] 1)
 (foo/with-redefs [inc dec] 1))
