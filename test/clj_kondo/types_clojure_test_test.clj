(ns clj-kondo.types-clojure-test-test
  (:require
   [clj-kondo.test-utils :as tu :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is testing]]))

(deftest clojure-test-is-macro-test
  (testing "is returns the result of the test expression - boolean"
    (assert-submaps2
     '({:file "<stdin>",
        :row 3,
        :col 21,
        :level :error,
        :message "Expected: number, received: boolean."})
     (lint! "(require '[clojure.test :refer [is]])
             (let [x (is (= 1 1))]
               (inc x))"
            {:linters {:type-mismatch {:level :error}}})))

  (testing "is returns boolean - can be used in if"
    (is (empty? (lint! "(require '[clojure.test :refer [is]])
                        (let [x (is (pos? 5))]
                          (if x :ok :fail))"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "is returns the actual value - number"
    (is (empty? (lint! "(require '[clojure.test :refer [is]])
                        (let [x (is (+ 1 2))]
                          (inc x))"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "is with string message"
    (is (empty? (lint! "(require '[clojure.test :refer [is]])
                        (is (= 1 1) \"should be equal\")"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "is requires string message in second argument"
    (assert-submaps2
     '({:file "<stdin>",
        :row 2,
        :col 26,
        :level :warning,
        :message "Test assertion message should be a string"})
     (lint! "(require '[clojure.test :refer [is]])
             (is (= 1 1) 42)"
            {:linters {:test-assertion-string-arg {:level :warning}}})))

  (testing "is with non-string message when linter disabled"
    (is (empty? (lint! "(require '[clojure.test :refer [is]])
                        (is (= 1 1) 42)
                        (is (= 2 2) [\"not\" \"equal\"])
                        (is (= 3 3) {:msg \"not equal\"})"
                       {:linters {:test-assertion-string-arg {:level :off}}}))))

  (testing "is accepts non-string message types when linter off"
    (is (empty? (lint! "(require '[clojure.test :refer [is]])
                        (is (= 200 (:status response)) {:request request :response response})"
                       {:linters {:test-assertion-string-arg {:level :off}}}))))

  (testing "is accepts any test expression"
    (is (empty? (lint! "(require '[clojure.test :refer [is]])
                        (is true)
                        (is (pos? 5))
                        (is (= 1 1))
                        (is (string? \"hello\"))"
                       {:linters {:type-mismatch {:level :error}}})))))

(deftest clojure-test-testing-macro-test
  (testing "testing returns the result of the last form"
    (is (empty? (lint! "(require '[clojure.test :refer [testing]])
                        (let [x (testing \"some test\" 42)]
                          (inc x))"
                       {:linters {:type-mismatch {:level :error}}})))
    (is (empty? (lint! "(require '[clojure.test :refer [testing is]])
                        (testing \"nested\"
                          (is (= 1 1))
                          (is (= 2 2)))"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "testing returns last form - type mismatch detection"
    (assert-submaps2
     '({:file "<stdin>",
        :row 3,
        :col 22,
        :level :error,
        :message "Expected: string, received: positive integer."})
     (lint! "(require '[clojure.test :refer [testing]])
             (let [x (testing \"some test\" 42)]
               (subs x 0 5))"
            {:linters {:type-mismatch {:level :error}}}))))

(deftest clojure-test-deftest-test
  (testing "deftest defines a test var"
    (is (empty? (lint! "(require '[clojure.test :refer [deftest is]])
                        (deftest my-test
                          (is (= 1 1)))"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "deftest result cannot be used as number"
    (assert-submaps2
     '({:file "<stdin>",
        :row 3,
        :col 21,
        :level :error,
        :message "Expected: number, received: var."})
     (lint! "(require '[clojure.test :refer [deftest is]])
             (let [x (deftest my-test (is (= 1 1)))]
               (inc x))"
            {:linters {:type-mismatch {:level :error}}})))

  (testing "deftest- creates private test"
    (is (empty? (lint! "(require '[clojure.test :refer [deftest- is]])
                        (deftest- private-test
                          (is (= 1 1)))"
                       {:linters {:type-mismatch {:level :error}}})))))

(deftest clojure-test-thrown-test
  (testing "thrown? returns the exception"
    (is (empty? (lint! "(require '[clojure.test :refer [is thrown?]])
                        (let [e (is (thrown? Exception (throw (Exception. \"test\"))))]
                          (.getMessage e))"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "thrown? result cannot be used as number"
    (assert-submaps2
     '({:file "<stdin>",
        :row 3,
        :col 21,
        :level :error,
        :message "Expected: number, received: throwable."})
     (lint! "(require '[clojure.test :refer [is thrown?]])
             (let [e (is (thrown? Exception (throw (Exception. \"test\"))))]
               (inc e))"
            {:linters {:type-mismatch {:level :error}}})))

  (testing "thrown-with-msg? returns the exception"
    (is (empty? (lint! "(require '[clojure.test :refer [is thrown-with-msg?]])
                        (let [e (is (thrown-with-msg? Exception #\"test\"
                                      (throw (Exception. \"test\"))))]
                          (.getMessage e))"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "thrown-with-msg? result cannot be used as string"
    (assert-submaps2
     '({:file "<stdin>",
        :row 4,
        :col 22,
        :level :error,
        :message "Expected: string, received: throwable."})
     (lint! "(require '[clojure.test :refer [is thrown-with-msg?]])
             (let [e (is (thrown-with-msg? Exception #\"test\"
                           (throw (Exception. \"test\"))))]
               (subs e 0 5))"
            {:linters {:type-mismatch {:level :error}}}))))

(deftest clojure-test-run-tests-test
  (testing "run-tests returns a map"
    (is (empty? (lint! "(require '[clojure.test :refer [run-tests]])
                        (let [result (run-tests)]
                          (:pass result))"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "run-tests accepts namespace symbols"
    (is (empty? (lint! "(require '[clojure.test :refer [run-tests]])
                        (let [result (run-tests 'my.test.ns 'another.test.ns)]
                          (:pass result))"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "run-tests requires symbol arguments for namespaces"
    (assert-submaps2
     '({:file "<stdin>",
        :row 2,
        :col 25,
        :level :error,
        :message "Expected: symbol, received: string."})
     (lint! "(require '[clojure.test :refer [run-tests]])
             (run-tests \"my.test.ns\")"
            {:linters {:type-mismatch {:level :error}}})))

  (testing "run-tests returns a map - cannot use as number"
    (assert-submaps2
     '({:file "<stdin>",
        :row 3,
        :col 21,
        :level :error,
        :message "Expected: number, received: map."})
     (lint! "(require '[clojure.test :refer [run-tests]])
             (let [result (run-tests)]
               (inc result))"
            {:linters {:type-mismatch {:level :error}}})))

  (testing "run-all-tests returns a map"
    (is (empty? (lint! "(require '[clojure.test :refer [run-all-tests]])
                        (let [result (run-all-tests)]
                          (:fail result))"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "run-all-tests returns a map - cannot use as string"
    (assert-submaps2
     '({:file "<stdin>",
        :row 3,
        :col 22,
        :level :error,
        :message "Expected: string, received: map."})
     (lint! "(require '[clojure.test :refer [run-all-tests]])
             (let [result (run-all-tests)]
               (subs result 0 5))"
            {:linters {:type-mismatch {:level :error}}})))

  (testing "test-ns returns a map"
    (is (empty? (lint! "(require '[clojure.test :refer [test-ns]])
                        (let [result (test-ns 'some.namespace)]
                          (:error result))"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "test-ns requires symbol argument for namespace"
    (assert-submaps2
     '({:file "<stdin>",
        :row 2,
        :col 23,
        :level :error,
        :message "Expected: symbol, received: string."})
     (lint! "(require '[clojure.test :refer [test-ns]])
             (test-ns \"some.namespace\")"
            {:linters {:type-mismatch {:level :error}}})))

  (testing "test-ns returns a map - cannot use as number"
    (assert-submaps2
     '({:file "<stdin>",
        :row 3,
        :col 21,
        :level :error,
        :message "Expected: number, received: map."})
     (lint! "(require '[clojure.test :refer [test-ns]])
             (let [result (test-ns 'some.namespace)]
               (inc result))"
            {:linters {:type-mismatch {:level :error}}}))))

(deftest clojure-test-successful-test
  (testing "successful? returns boolean"
    (is (empty? (lint! "(require '[clojure.test :refer [successful? run-tests]])
                        (if (successful? (run-tests))
                          :pass
                          :fail)"
                       {:linters {:type-mismatch {:level :error}}})))
    (assert-submaps2
     '({:file "<stdin>",
        :row 2,
        :col 27,
        :level :error,
        :message "Expected: map, received: positive integer."})
     (lint! "(require '[clojure.test :refer [successful?]])
             (successful? 42)"
            {:linters {:type-mismatch {:level :error}}})))

  (testing "successful? returns boolean - cannot use as number"
    (assert-submaps2
     '({:file "<stdin>",
        :row 3,
        :col 21,
        :level :error,
        :message "Expected: number, received: boolean."})
     (lint! "(require '[clojure.test :refer [successful? run-tests]])
             (let [result (successful? (run-tests))]
               (inc result))"
            {:linters {:type-mismatch {:level :error}}}))))

(deftest clojure-test-are-macro-test
  (testing "are expands to multiple is calls and returns boolean"
    (is (empty? (lint! "(require '[clojure.test :refer [are]])
                        (if (are [x y] (= x y)
                              2 (+ 1 1)
                              4 (* 2 2))
                          :pass
                          :fail)"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "are returns boolean - cannot use as number"
    (assert-submaps2
     '({:file "<stdin>",
        :row 5,
        :col 21,
        :level :error,
        :message "Expected: number, received: boolean."})
     (lint! "(require '[clojure.test :refer [are]])
             (let [result (are [x y] (= x y)
                            2 (+ 1 1)
                            4 (* 2 2))]
               (inc result))"
            {:linters {:type-mismatch {:level :error}}}))))

(deftest clojure-test-fixtures-test
  (testing "compose-fixtures returns a function"
    (is (empty? (lint! "(require '[clojure.test :refer [compose-fixtures]])
                        (let [f1 (fn [t] (t))
                              f2 (fn [t] (t))
                              composed (compose-fixtures f1 f2)]
                          (composed (fn [] :done)))"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "compose-fixtures requires function arguments"
    (assert-submaps2
     '({:file "<stdin>",
        :row 2,
        :col 32,
        :level :error,
        :message "Expected: function, received: positive integer."})
     (lint! "(require '[clojure.test :refer [compose-fixtures]])
             (compose-fixtures 1 (fn [t] (t)))"
            {:linters {:type-mismatch {:level :error}}})))

  (testing "compose-fixtures result cannot be used as number"
    (assert-submaps2
     '({:file "<stdin>",
        :row 5,
        :col 21,
        :level :error,
        :message "Expected: number, received: function."})
     (lint! "(require '[clojure.test :refer [compose-fixtures]])
             (let [f1 (fn [t] (t))
                   f2 (fn [t] (t))
                   composed (compose-fixtures f1 f2)]
               (inc composed))"
            {:linters {:type-mismatch {:level :error}}})))

  (testing "join-fixtures returns a function"
    (is (empty? (lint! "(require '[clojure.test :refer [join-fixtures]])
                        (let [fixtures [(fn [t] (t)) (fn [t] (t))]
                              joined (join-fixtures fixtures)]
                          (joined (fn [] :done)))"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "join-fixtures requires seqable argument"
    (assert-submaps2
     '({:file "<stdin>",
        :row 2,
        :col 29,
        :level :error,
        :message "Expected: seqable collection, received: positive integer."})
     (lint! "(require '[clojure.test :refer [join-fixtures]])
             (join-fixtures 42)"
            {:linters {:type-mismatch {:level :error}}})))

  (testing "join-fixtures result cannot be used as number"
    (assert-submaps2
     '({:file "<stdin>",
        :row 4,
        :col 21,
        :level :error,
        :message "Expected: number, received: function."})
     (lint! "(require '[clojure.test :refer [join-fixtures]])
             (let [fixtures [(fn [t] (t)) (fn [t] (t))]
                   joined (join-fixtures fixtures)]
               (inc joined))"
            {:linters {:type-mismatch {:level :error}}}))))

(deftest clojure-test-test-var-test
  (testing "test-var accepts a var"
    (is (empty? (lint! "(require '[clojure.test :refer [test-var deftest is]])
                        (deftest my-test (is (= 1 1)))
                        (test-var #'my-test)"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "test-var requires a var argument"
    (assert-submaps2
     '({:file "<stdin>",
        :row 3,
        :col 24,
        :level :error,
        :message "Expected: var, received: string."})
     (lint! "(require '[clojure.test :refer [test-var deftest is]])
             (deftest my-test (is (= 1 1)))
             (test-var \"not-a-var\")"
            {:linters {:type-mismatch {:level :error}}}))))

(deftest clojure-test-reporting-test
  (testing "do-report accepts a map"
    (is (empty? (lint! "(require '[clojure.test :refer [do-report]])
                        (do-report {:type :pass :message \"ok\"})"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "do-report requires a map argument"
    (assert-submaps2
     '({:file "<stdin>",
        :row 2,
        :col 25,
        :level :error,
        :message "Expected: map, received: string."})
     (lint! "(require '[clojure.test :refer [do-report]])
             (do-report \"not a map\")"
            {:linters {:type-mismatch {:level :error}}})))

  (testing "report accepts a map"
    (is (empty? (lint! "(require '[clojure.test :refer [report]])
                        (report {:type :fail :message \"failed\"})"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "report requires a map argument"
    (assert-submaps2
     '({:file "<stdin>",
        :row 2,
        :col 22,
        :level :error,
        :message "Expected: map, received: positive integer."})
     (lint! "(require '[clojure.test :refer [report]])
             (report 42)"
            {:linters {:type-mismatch {:level :error}}})))

  (testing "testing-vars-str returns string"
    (is (empty? (lint! "(require '[clojure.test :refer [testing-vars-str]])
                        (let [s (testing-vars-str {:file \"test.clj\" :line 10})]
                          (subs s 0 5))"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "testing-vars-str returns string - cannot use as number"
    (assert-submaps2
     '({:file "<stdin>",
        :row 3,
        :col 21,
        :level :error,
        :message "Expected: number, received: string."})
     (lint! "(require '[clojure.test :refer [testing-vars-str]])
             (let [s (testing-vars-str {:file \"test.clj\" :line 10})]
               (inc s))"
            {:linters {:type-mismatch {:level :error}}})))

  (testing "testing-vars-str requires a map argument"
    (assert-submaps2
     '({:file "<stdin>",
        :row 2,
        :col 32,
        :level :error,
        :message "Expected: map, received: string."})
     (lint! "(require '[clojure.test :refer [testing-vars-str]])
             (testing-vars-str \"not a map\")"
            {:linters {:type-mismatch {:level :error}}})))

  (testing "testing-contexts-str returns string"
    (is (empty? (lint! "(require '[clojure.test :refer [testing-contexts-str]])
                        (let [s (testing-contexts-str)]
                          (count s))"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "testing-contexts-str returns string - cannot use as number"
    (assert-submaps2
     '({:file "<stdin>",
        :row 3,
        :col 21,
        :level :error,
        :message "Expected: number, received: string."})
     (lint! "(require '[clojure.test :refer [testing-contexts-str]])
             (let [s (testing-contexts-str)]
               (inc s))"
            {:linters {:type-mismatch {:level :error}}}))))

(deftest clojure-test-function-predicate-test
  (testing "function? returns boolean"
    (is (empty? (lint! "(require '[clojure.test :refer [function?]])
                        (if (function? inc)
                          :is-fn
                          :not-fn)"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "function? returns boolean - cannot use as number"
    (assert-submaps2
     '({:file "<stdin>",
        :row 3,
        :col 21,
        :level :error,
        :message "Expected: number, received: boolean."})
     (lint! "(require '[clojure.test :refer [function?]])
             (let [is-fn (function? inc)]
               (inc is-fn))"
            {:linters {:type-mismatch {:level :error}}}))))

(deftest clojure-test-integration-test
  (testing "complete test definition and execution"
    (is (empty? (lint! "(require '[clojure.test :refer [deftest is testing run-tests successful?]])
                        (deftest math-test
                          (testing \"addition\"
                            (is (= 4 (+ 2 2))))
                          (testing \"multiplication\"
                            (is (= 6 (* 2 3)))))
                        (let [results (run-tests)]
                          (if (successful? results)
                            :all-passed
                            :some-failed))"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "nested testing blocks"
    (is (empty? (lint! "(require '[clojure.test :refer [deftest is testing]])
                        (deftest nested-test
                          (testing \"outer\"
                            (testing \"inner 1\"
                              (is (= 1 1)))
                            (testing \"inner 2\"
                              (is (= 2 2)))))"
                       {:linters {:type-mismatch {:level :error}}})))))

(deftest clojure-test-with-test-test
  (testing "with-test returns a var"
    (is (empty? (lint! "(require '[clojure.test :refer [with-test is]])
                        (with-test
                          (defn my-fn [x] (* x 2))
                          (is (= 4 (my-fn 2))))"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "with-test result cannot be used as number"
    (assert-submaps2
     '({:file "<stdin>",
        :row 5,
        :col 32,
        :level :error,
        :message "Expected: number, received: var."})
     (lint! "(require '[clojure.test :refer [with-test is]])
                        (let [x (with-test
                                  (defn my-fn [x] (* x 2))
                                  (is (= 4 (my-fn 2))))]
                          (inc x))"
            {:linters {:type-mismatch {:level :error}}}))))

(deftest clojure-test-set-test-test
  (testing "set-test sets test metadata"
    (is (empty? (lint! "(require '[clojure.test :refer [set-test is]])
                        (defn my-fn [x] (* x 2))
                        (set-test my-fn
                          (is (= 4 (my-fn 2))))"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "set-test returns a map (the var metadata)"
    (is (empty? (lint! "(require '[clojure.test :refer [set-test is]])
                        (defn my-fn [x] (* x 2))
                        (let [m (set-test my-fn (is (= 4 (my-fn 2))))]
                          (:test m))"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "set-test result cannot be used as number"
    (assert-submaps2
     '({:file "<stdin>",
        :row 3,
        :col 30,
        :level :error,
        :message "Expected: number, received: map."})
     (lint! "(require '[clojure.test :refer [set-test is]])
                        (defn my-fn [x] (* x 2))
                        (inc (set-test my-fn (is (= 4 (my-fn 2)))))"
            {:linters {:type-mismatch {:level :error}}}))))

(deftest clojure-test-use-fixtures-test
  (testing "use-fixtures with :each"
    (is (empty? (lint! "(require '[clojure.test :refer [use-fixtures]])
                        (defn my-fixture [f] (f))
                        (use-fixtures :each my-fixture)"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "use-fixtures with :once"
    (is (empty? (lint! "(require '[clojure.test :refer [use-fixtures]])
                        (defn fixture1 [f] (f))
                        (defn fixture2 [f] (f))
                        (use-fixtures :once fixture1 fixture2)"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "use-fixtures returns a map (namespace metadata)"
    (is (empty? (lint! "(require '[clojure.test :refer [use-fixtures]])
                        (defn my-fixture [f] (f))
                        (let [m (use-fixtures :each my-fixture)]
                          (:each-fixtures m))"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "use-fixtures result cannot be used as number"
    (assert-submaps2
     '({:file "<stdin>",
        :row 3,
        :col 30,
        :level :error,
        :message "Expected: number, received: map."})
     (lint! "(require '[clojure.test :refer [use-fixtures]])
                        (defn my-fixture [f] (f))
                        (inc (use-fixtures :each my-fixture))"
            {:linters {:type-mismatch {:level :error}}}))))

(deftest clojure-test-test-vars-test
  (testing "test-vars accepts sequence of vars"
    (is (empty? (lint! "(require '[clojure.test :refer [test-vars deftest is]])
                        (deftest my-test (is (= 1 1)))
                        (test-vars [#'my-test])"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "test-vars returns nil"
    (is (empty? (lint! "(require '[clojure.test :refer [test-vars deftest is]])
                        (deftest my-test (is (= 1 1)))
                        (let [x (test-vars [#'my-test])]
                          (when-not x :ok))"
                       {:linters {:type-mismatch {:level :error}}})))))

(deftest clojure-test-test-all-vars-test
  (testing "test-all-vars accepts namespace symbol"
    (is (empty? (lint! "(require '[clojure.test :refer [test-all-vars]])
                        (test-all-vars 'my.test.ns)"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "test-all-vars returns nil"
    (is (empty? (lint! "(require '[clojure.test :refer [test-all-vars]])
                        (let [x (test-all-vars 'my.test.ns)]
                          (when-not x :ok))"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "test-all-vars requires symbol argument"
    (assert-submaps2
     '({:file "<stdin>",
        :row 2,
        :col 40,
        :level :error,
        :message "Expected: symbol, received: positive integer."})
     (lint! "(require '[clojure.test :refer [test-all-vars]])
                        (test-all-vars 42)"
            {:linters {:type-mismatch {:level :error}}})))

  (testing "test-all-vars result cannot be used as number"
    (assert-submaps2
     '({:file "<stdin>",
        :row 2,
        :col 30,
        :level :error,
        :message "Expected: number, received: nil."})
     (lint! "(require '[clojure.test :refer [test-all-vars]])
                        (inc (test-all-vars 'my.test.ns))"
            {:linters {:type-mismatch {:level :error}}}))))

(deftest clojure-test-get-possibly-unbound-var-test
  (testing "get-possibly-unbound-var accepts a var"
    (is (empty? (lint! "(require '[clojure.test :refer [get-possibly-unbound-var]])
                        (def my-var 42)
                        (get-possibly-unbound-var #'my-var)"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "get-possibly-unbound-var returns a nilable var"
    (is (empty? (lint! "(require '[clojure.test :refer [get-possibly-unbound-var]])
                        (def my-var 42)
                        (let [x (get-possibly-unbound-var #'my-var)]
                          (if x (deref x) :nil))"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "get-possibly-unbound-var can return nil for unbound var"
    (is (empty? (lint! "(require '[clojure.test :refer [get-possibly-unbound-var]])
                        (let [x (get-possibly-unbound-var #'nonexistent-var)]
                          (when-not x :nil-returned))"
                       {:linters {:type-mismatch {:level :error}}}))))

  (testing "get-possibly-unbound-var requires a var argument"
    (assert-submaps2
     '({:file "<stdin>",
        :row 3,
        :col 51,
        :level :error,
        :message "Expected: var, received: positive integer."})
     (lint! "(require '[clojure.test :refer [get-possibly-unbound-var]])
                        (def my-var 42)
                        (get-possibly-unbound-var 42)"
            {:linters {:type-mismatch {:level :error}}}))))
