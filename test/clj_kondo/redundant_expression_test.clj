(ns clj-kondo.redundant-expression-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :refer [deftest is testing]]))

(deftest shadowed-var-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 5, :level :warning, :message "Redundant expression: 1"})
   (lint! "(do 1 2) "))
  (is (empty?
       (lint! "(fn [] (:foo {}))"))))
