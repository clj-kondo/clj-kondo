(ns fail
  (:require [clojure.test :refer [deftest]]))

(deftest my-test
  (let [x 1]
    (let [y 2]
      (assert (not=  x y)))))
