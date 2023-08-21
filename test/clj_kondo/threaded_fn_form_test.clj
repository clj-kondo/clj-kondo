(ns clj-kondo.threaded-fn-form-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest threaded-function-form-test
  (testing "fn literal in thread-first form is a warning"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 9, :level :warning,
        :message "threading a value into the name slot of an anonymous fn"})
     (lint! "(-> foo (fn [x] (inc x)))")))
  (testing "fn* literal in thread-first form is a warning"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 9, :level :warning,
        :message "threading a value into the name slot of an anonymous fn"})
     (lint! "(-> bar (fn* [y] (dec y)))")))
  (testing "reader fn in thread-first form is a warning"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 9, :level :warning,
        :message "threading a value into the name slot of an anonymous fn"})
     (lint! "(-> baz #(inc %))")))
  (testing "double wrapped fn literal is not a warning"
    (is (empty? (lint! "(-> qux ((fn [x] (inc x))))")))
    (is (empty? (lint! "(-> qux (#(inc %)))")))))
