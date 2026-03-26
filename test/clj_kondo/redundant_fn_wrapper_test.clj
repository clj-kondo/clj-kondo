(ns clj-kondo.redundant-fn-wrapper-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps]]
            [clojure.test :refer [deftest is testing]]))

(deftest redundant-fn-wrapper-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 1, :level :warning, :message "Redundant fn wrapper"})
   (lint! "#(empty %)" {:linters {:redundant-fn-wrapper {:level :warning}}}))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 1, :level :warning, :message "Redundant fn wrapper"})
   (lint! "#(empty %1)" {:linters {:redundant-fn-wrapper {:level :warning}}}))
  (assert-submaps
    '({:file "<stdin>", :row 1, :col 6, :level :warning, :message "Redundant fn wrapper"})
    (lint! "(map #(:a %) uuids)" {:linters {:redundant-fn-wrapper {:level :warning}}}))
  (assert-submaps
    '({:file "<stdin>", :row 1, :col 19, :level :warning, :message "Redundant fn wrapper"})
   (lint! "(let [i inc] (map #(i %) uuids))" {:linters {:redundant-fn-wrapper {:level :warning}}})))

(deftest no-redundant-fn-wrapper-test
  (is (empty?
       (lint! "(require '[clojure.test :refer [is]]) #(is %)"
              {:linters {:redundant-fn-wrapper {:level :warning}}})))
  (is (empty?
       (lint! "#(clojure.lang.RT/equiv %)"
              {:linters {:redundant-fn-wrapper {:level :warning}}})))
  (is (empty?
       (lint! "(fn [x] (inc x) (dec x))"
              {:linters {:redundant-fn-wrapper {:level :warning}}})))
  (is (empty?
       (lint! "(fn [x] {:pre [(odd? x)]} (- x))"
              {:linters {:redundant-fn-wrapper {:level :warning}}})))
  (is (empty?
       (lint! "(let [nsm {}]
                 (fn [sym]
                   `(.println
                      (RT/errPrintWriter)
                      ~(nsm sym))))"
              {:linters {:redundant-fn-wrapper {:level :warning}}})))
  (is (empty?
       (lint! "(declare x) (.then x #(:foo %))"
              {:linters {:redundant-fn-wrapper {:level :warning}}}
              "--lang" "cljs")))
  (is (empty?
       (lint! "(fn [x] #?(:cljs (identity x) :clj (identity (* x 2))))"
              {:linters {:redundant-fn-wrapper {:level :warning}}}
              "--filename" "foo.cljc")))
  (testing "inlined function"
    (is (empty?
         (lint! "(fn [x y] (+ x y))"
                {:linters {:redundant-fn-wrapper {:level :warning}}})))))
