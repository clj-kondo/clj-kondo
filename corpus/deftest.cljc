(ns deftest
  (:require [clojure.test :refer [deftest is are]])
  (:import clojure.lang.ExceptionInfo))

(deftest are-test
  (are [?a ?b]
      (is (= ?a (dec ?b)))
    1 2
    10 11
    14 15))

(deftest thown-with-msg-test
  (is (thrown-with-msg?
       ExceptionInfo #"uh oh"
       (throw (ex-info "uh oh" {}))))
  (is (thrown? ExceptionInfo (throw (ex-info "uh oh" {})))))
