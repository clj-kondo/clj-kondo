(ns my-test
  (:require [foo :as foo]))

(defn my-test [& xs]
  (prn xs))

(let [x 1]
  (my-test
   (with-redefs [inc dec] 1)
   (foo/with-redefs [inc dec] 1)
   (inc 1)
   (dude 1)
   x))
