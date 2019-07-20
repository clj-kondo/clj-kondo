(ns redefined-deftest
  (:require [clojure.test :refer [deftest]]))

(deftest)

(deftest foo)
(deftest foo) ;; <- redefined test warning

(foo 1)


