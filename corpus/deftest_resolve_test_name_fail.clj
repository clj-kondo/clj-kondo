(ns deftest-resolve-test-name-fail
  (:require [clojure.string :refer :all]
            [clojure.test :refer :all]))
(deftest my-test
  (is (blank? ""))
  (is (thrown? (throw (Exception.)))))
