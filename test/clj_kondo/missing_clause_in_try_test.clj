(ns clj-kondo.missing-clause-in-try-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest missing-clause-in-try-error-test
  (testing "test linting error of missing clause in try for clojure"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Missing catch or finally in try"})
     (lint! "(try (/ 1 0))")))

  (testing "test linting error of missing clause in try with several exprs"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Missing catch or finally in try"})
     (lint! "(try (/ 1 0) (prn \"test\"))")))

  (testing "test linting error of missing clause in try for cljs"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Missing catch or finally in try"})
     (lint! "(try (/ 1 0))" "--lang" "cljs")))
  (testing "test linting error for first level in nested try"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Missing catch or finally in try"})
     (lint! "(try
               (/ 1 0)
               (try
                 (prn 11)
                 (finally Exception e (prn 22))))")))
  (testing "test linting error on second level in nested try in catch"
    (assert-submaps
     '({:file "<stdin>", :row 4, :col 18, :level :warning,
        :message "Missing catch or finally in try"})
     (lint! "(try
               (/ 1 0)
               (catch Exception e
                 (try
                   (prn 11))))")))
  (testing "test linting error for both levels in nested try"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Missing catch or finally in try"}
       {:file "<stdin>", :row 3, :col 16, :level :warning,
        :message "Missing catch or finally in try"})
     (lint! "(try
               (/ 1 0)
               (try
                 (prn 11)))"))))

(deftest missing-clause-in-try-valid-test
  (testing "test linting try with catch"
    (is (empty? (lint! "(try
                          (/ 1 0)
                          (catch Exception e (prn \"Caught\")))"))))
  (testing "test linting try with finally"
    (is (empty? (lint! "(try
                          (/ 1 0)
                          (finally (prn \"Do something always\")))"))))
  (testing "test linting try with multiple catch"
    (is (empty? (lint! "(try
                          (/ 1 0)
                          (catch AssertionError e (prn \"Assertion\"))
                          (catch Exception e (prn \"Caught\")))"))))
  (testing "test linting try with multiple catch and finally"
    (is (empty? (lint! "(try
                          (/ 1 0)
                          (catch AssertionError e (prn \"Assertion\"))
                          (catch Exception e (prn \"Caught\"))
                          (finally (prn \"Do something always\")))"))))
  (testing "test linting nested try in body with clauses"
    (is (empty? (lint! "(try
                          (/ 1 0)
                          (try
                            (prn 11)
                            (finally Exception e (prn 22)))
                          (catch Exception e (prn \"Caought\")))"))))
  (testing "test linting nested try in catch with clauses"
    (is (empty? (lint! "(try
                          (/ 1 0)
                          (catch Exception e
                            (try
                              (prn 11)
                              (finally Exception e (prn 22)))))")))))
