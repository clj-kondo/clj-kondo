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
     (lint! "(try (/ 1 0))" "--lang" "cljs"))))

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
                          (finally (prn \"Do something always\")))")))))
