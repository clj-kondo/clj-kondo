(ns clj-kondo.var-same-except-case-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps2] :rename {assert-submaps2 assert-submaps}]
   [clojure.test :refer [deftest testing]]))

(deftest var-same-except-case-test
  (testing "test linting error for vars with names that differ only in case"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 11, :level :warning,
        :message "Foo differs only in case from foo"})
     (lint! "(def foo) (def Foo)"))))
