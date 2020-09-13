(ns deftest-resolve-test-name-pass
  (:require [clojure.test :refer :all]
            [clojure.string :refer :all]))
(deftest my-test
  (is (blank? ""))
  (is (thrown? (throw (Exception.)))))
