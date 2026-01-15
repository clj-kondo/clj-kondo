(ns clj-kondo.equals-expected-position-test 
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is testing]]))
   

(deftest equals-expected-order-test
  (testing "warns when actual is first in ="
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 14, :level :warning, :message "Write expected value first"})
     (lint! "(= (+ 1 2 3) 6)" {:linters {:equals-expected-position {:level :warning}}})))
  
  (testing "does not warn when expected is first in ="
    (is (empty? (lint! "(= 6 (+ 1 2 3))" {:linters {:equals-expected-position {:level :warning}}})))
    (is (empty? (lint! "(= 6 (+ 1 2 3))" {:linters {:equals-expected-position {:level :warning}}}))))
  
  (testing "only warns in test assertions when configured"
    (assert-submaps2
     '({:file "<stdin>", :row 2, :col 41, :level :warning, :message "Write expected value first"})
     (lint! "(require '[clojure.test :refer [is]])
                       (is (= (+ 1 2 3) 6))
                       (= (+ 1 2 3) 6)" {:linters {:equals-expected-position {:only-in-test-assertion true
                                                                              :level :warning}}})))
  (testing "not= tests"
    (testing "warns when actual is first in not="
      (assert-submaps2
       '({:file "<stdin>", :row 1, :col 9, :level :warning, :message "Write expected value first"})
       (lint! "(not= x 1)" {:linters {:equals-expected-position {:level :warning}}})))
    (testing "does not warn when expected is first in not="
      (is (empty? (lint! "(not= 1 x)" {:linters {:equals-expected-position {:level :warning}}}))))
    (testing "only warns in test assertions for not= when configured"
      (assert-submaps2
       '({:file "<stdin>", :row 2, :col 36, :level :warning, :message "Write expected value first"})
       (lint! "(require '[clojure.test :refer [is]])
                       (is (not= x 1))
                       (not= x 1)" {:linters {:equals-expected-position {:only-in-test-assertion true
                                                                         :level :warning}}})))))