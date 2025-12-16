(ns clj-kondo.unquote-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest unquote-outside-syntax-quote-test
  (testing "unquote outside syntax-quote"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 1, :level :warning, :message "Unquote (~) used outside syntax-quote"})
     (lint! "~x" {:linters {:unquote-outside-syntax-quote {:level :warning}}})))
  (testing "unquote-splicing outside syntax-quote"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 1, :level :warning, :message "Unquote-splicing (~@) used outside syntax-quote"})
     (lint! "~@x" {:linters {:unquote-outside-syntax-quote {:level :warning}}})))
  (testing "unquote inside syntax-quote is allowed"
    (is (empty? (lint! "`(foo ~x)" {:linters {:unquote-outside-syntax-quote {:level :warning}}}))))
  (testing "unquote-splicing inside syntax-quote is allowed"
    (is (empty? (lint! "`(foo ~@xs)" {:linters {:unquote-outside-syntax-quote {:level :warning}}}))))
  (testing "quoted unquote warns"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 2, :level :warning, :message "Unquote (~) used outside syntax-quote"})
     (lint! "'~x" {:linters {:unquote-outside-syntax-quote {:level :warning}}}))))
(testing "quoted unquote-splicing warns"
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 2, :level :warning, :message "Unquote-splicing (~@) used outside syntax-quote"})
   (lint! "'~@x" {:linters {:unquote-outside-syntax-quote {:level :warning}}})))
(testing "linter can be disabled"
  (is (empty? (lint! "~x" {:linters {:unquote-outside-syntax-quote {:level :off}}})))
  (is (empty? (lint! "'~x" {:linters {:unquote-outside-syntax-quote {:level :off}}}))))
