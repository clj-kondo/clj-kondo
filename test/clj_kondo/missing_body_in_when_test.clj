(ns clj-kondo.missing-body-in-when-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest missing-body-in-when-error-test
  (testing "test linting error of missing body in when for clojure"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Missing body in when"})
     (lint! "(when (> 1 0))")))
  (testing "test linting error of missing body in when for cljs"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Missing body in when"})
     (lint! "(when true)" "--lang" "cljs")))
  (testing "test linting error of missing body in nested when"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 15, :level :warning,
        :message "Missing body in when"})
     (lint! "(when (> 1 0) (when (= 1 2)))"))))

(deftest missing-body-in-when-valid-test
  (testing "test linting when with condition and body"
    (is (empty? (lint! "(when (> 1 0) (prn 1))"))))
  (testing "test linting when with multiple expresion in body"
    (is (empty? (lint! "(when true (prn 1) (+ 1 1))"))))
  (testing "test linting nested when"
    (is (empty? (lint! "(when (> 1 0) (when (prn 1) (+ 1 1)))")))))
