(ns clj-kondo.unquote-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps2]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest unquote-outside-syntax-quote-test
  (testing "unquote outside syntax-quote"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 1
        :level :warning
        :message "Unquote (~) used outside syntax-quote"})
     (lint! "~x" {:linters {:unquote-not-syntax-quoted
                            {:level :warning}}})))
  (testing "unquote-splicing outside syntax-quote"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 1
        :level :warning
        :message "Unquote-splicing (~@) used outside syntax-quote"})
     (lint! "~@x" {:linters {:unquote-not-syntax-quoted
                             {:level :warning}}})))
  (testing "unquote outside syntax-quote by double unquote"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 14
        :level :warning
        :message "Unquote (~) used outside syntax-quote"})
     (lint! "(def x 1) `[~~x]"
            {:linters {:unquote-not-syntax-quoted
                       {:level :warning}}})))
  (testing "unquote inside syntax-quote is allowed"
    (is (empty? (lint! "`(foo ~x)" {:linters {:unquote-not-syntax-quoted
                                              {:level :warning}}}))))
  (testing "unquote-splicing inside syntax-quote is allowed"
    (is (empty? (lint! "`(foo ~@xs)" {:linters {:unquote-not-syntax-quoted
                                                {:level :warning}}}))))
  (testing "quoted unquote warns"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 2
        :level :warning
        :message "Unquote (~) used outside syntax-quote"})
     (lint! "'~x" {:linters {:unquote-not-syntax-quoted
                             {:level :warning}}}))))
(testing "quoted unquote-splicing warns"
  (assert-submaps2
   '({:file "<stdin>"
      :row 1
      :col 2
      :level :warning
      :message "Unquote-splicing (~@) used outside syntax-quote"})
   (lint! "'~@x" {:linters {:unquote-not-syntax-quoted {:level :warning}}})))
(testing "linter can be disabled"
  (is (empty? (lint! "~x" {:linters {:unquote-not-syntax-quoted
                                     {:level :off}}})))
  (is (empty? (lint! "'~x" {:linters {:unquote-not-syntax-quoted
                                      {:level :off}}}))))
(testing "linter can be disabled in specific calls with config-in-call"
  (assert-submaps2
   '({:file "<stdin>"
      :row 7
      :col 1
      :level :warning
      :message "Unquote (~) used outside syntax-quote"})
   (lint! "(ns scratch
  {:clj-kondo/config '{:config-in-call {babashka2.process/$$ {:linters {:unquote-not-syntax-quoted {:level :off}}}}}})

(require '[babashka2.process :as proc])

(proc/$$ 1 ~2) ;; no warning here
~2  ;; warning" {:linters {:unquote-not-syntax-quoted {:level :warning}}})))
