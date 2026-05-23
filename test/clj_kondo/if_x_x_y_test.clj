(ns clj-kondo.if-x-x-y-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is testing]]))

(def ^:private config
  {:linters {:if-x-x-y {:level :warning}
             :missing-else-branch {:level :off}}})

(deftest if-x-x-y-test
  (testing "(if x x y) -> (or x y)"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "If condition and then branch are the same; use (or x y)"})
     (lint! "(if x x y)" config)))

  (testing "works in ClojureScript"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "If condition and then branch are the same; use (or x y)"})
     (lint! "(if x x y)" config "--lang" "cljs")))

  (testing "no false positives"
    (is (empty? (lint! "(if x z y)" config)))
    (is (empty? (lint! "(if x x)" config)))
    (is (empty? (lint! "(if (foo) (foo) y)" config)))
    (is (empty? (lint! "(if [1] [1] y)" config)))))

(deftest if-x-x-y-ignore-test
  (testing "off at default level"
    (is (empty? (lint! "(if x x y)"))))

  (testing "no warning when linter is disabled"
    (is (empty? (lint! "(if x x y)"
                       {:linters {:if-x-x-y {:level :off}}}))))

  (testing "linter-specific ignore suppresses finding"
    (is (empty? (lint! "#_{:clj-kondo/ignore [:if-x-x-y]} (if x x y)"
                       config))))

  (testing "bare ^:clj-kondo/ignore suppresses all linters"
    (is (empty? (lint! "#_:clj-kondo/ignore (if x x y)" config))))

  (testing "linter-specific ignore does not suppress unrelated linter"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 40, :level :warning,
        :message "If condition and then branch are the same; use (or x y)"})
     (lint! "#_{:clj-kondo/ignore [:invalid-arity]} (if x x y)" config))))
