(ns redefined-deftest
  (:require [clojure.test :refer [deftest is]]))

(deftest)

(deftest foo)
(deftest foo)

(foo 1)


