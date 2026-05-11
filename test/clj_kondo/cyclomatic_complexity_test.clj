(ns clj-kondo.cyclomatic-complexity-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps2]]
   [clojure.test :refer [deftest is testing]]))

(def ^:private config {:linters {:cyclomatic-complexity {:level :warning
                                                         :threshold 2}}})

(deftest basic-conditionals-test
  (testing "single if has complexity 2"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 2, exceeds threshold of 1. Consider breaking this into smaller functions."})
     (lint! "(defn foo [x] (if x 1 2))"
            {:linters {:cyclomatic-complexity {:level :warning
                                               :threshold 1}}})))

  (testing "no warning when under threshold"
    (is (empty? (lint! "(defn foo [x] (if x 1 2))" config)))))

(deftest nested-conditionals-test
  (testing "nested if has complexity 3"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 3, exceeds threshold of 2. Consider breaking this into smaller functions."})
     (lint! "(defn foo [x y] (if x (if y 1 2) 3))" config)))

  (testing "when has complexity 2"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 2, exceeds threshold of 1. Consider breaking this into smaller functions."})
     (lint! "(defn foo [x] (when x 1))"
            {:linters {:cyclomatic-complexity {:level :warning
                                               :threshold 1}}}))))

(deftest when-variants-test
  (testing "when-not has complexity 2"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 2, exceeds threshold of 1. Consider breaking this into smaller functions."})
     (lint! "(defn foo [x] (when-not x 1))"
            {:linters {:cyclomatic-complexity {:level :warning
                                               :threshold 1}}})))

  (testing "if-not has complexity 2"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 2, exceeds threshold of 1. Consider breaking this into smaller functions."})
     (lint! "(defn foo [x] (if-not x 1 2))"
            {:linters {:cyclomatic-complexity {:level :warning
                                               :threshold 1}}})))

  (testing "when-let has complexity 2"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 2, exceeds threshold of 1. Consider breaking this into smaller functions."})
     (lint! "(defn foo [x] (when-let [y x] y))"
            {:linters {:cyclomatic-complexity {:level :warning
                                               :threshold 1}}})))

  (testing "if-let has complexity 2"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 2, exceeds threshold of 1. Consider breaking this into smaller functions."})
     (lint! "(defn foo [x] (if-let [y x] y nil))"
            {:linters {:cyclomatic-complexity {:level :warning
                                               :threshold 1}}})))

  (testing "when-some has complexity 2"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 2, exceeds threshold of 1. Consider breaking this into smaller functions."})
     (lint! "(defn foo [x] (when-some [y x] y))"
            {:linters {:cyclomatic-complexity {:level :warning
                                               :threshold 1}}})))

  (testing "if-some has complexity 2"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 2, exceeds threshold of 1. Consider breaking this into smaller functions."})
     (lint! "(defn foo [x] (if-some [y x] y nil))"
            {:linters {:cyclomatic-complexity {:level :warning
                                               :threshold 1}}}))))

(deftest cond-test
  (testing "cond with 3 conditions has complexity 4"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 4, exceeds threshold of 3. Consider breaking this into smaller functions."})
     (lint! "(defn foo [x] (cond (< x 0) -1 (= x 0) 0 (> x 0) 1))"
            {:linters {:cyclomatic-complexity {:level :warning
                                               :threshold 3}}})))

  (testing "cond with :else doesn't add extra complexity"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 3, exceeds threshold of 2. Consider breaking this into smaller functions."})
     (lint! "(defn foo [x] (cond (< x 0) -1 (> x 0) 1 :else 0))" config))))

(deftest case-test
  (testing "case with 4 clauses has complexity 5"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 5, exceeds threshold of 4. Consider breaking this into smaller functions."})
     (lint! "(defn foo [x] (case x 1 :one 2 :two 3 :three 4 :four :other))"
            {:linters {:cyclomatic-complexity {:level :warning
                                               :threshold 4}}})))

  (testing "case counts non-default clauses only"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 3, exceeds threshold of 2. Consider breaking this into smaller functions."})
     (lint! "(defn foo [x] (case x 1 :one 2 :two :other))" config))))

(deftest condp-test
  (testing "condp with 3 tests has complexity 4"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 4, exceeds threshold of 3. Consider breaking this into smaller functions."})
     (lint! "(defn foo [x] (condp = x 1 :one 2 :two 3 :three :other))"
            {:linters {:cyclomatic-complexity {:level :warning
                                               :threshold 3}}}))))

(deftest logical-operators-test
  (testing "and with 3 operands has complexity 3"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 3, exceeds threshold of 2. Consider breaking this into smaller functions."})
     (lint! "(defn foo [x y z] (and x y z))" config)))

  (testing "or with 2 operands has complexity 2"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 2, exceeds threshold of 1. Consider breaking this into smaller functions."})
     (lint! "(defn foo [x y] (or x y))"
            {:linters {:cyclomatic-complexity {:level :warning
                                               :threshold 1}}})))

  (testing "nested and/or combines complexity"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 4, exceeds threshold of 3. Consider breaking this into smaller functions."})
     (lint! "(defn foo [a b c d] (and (or a b) (or c d)))"
            {:linters {:cyclomatic-complexity {:level :warning
                                               :threshold 3}}}))))

(deftest exception-handling-test
  (testing "try with 2 catch clauses has complexity 3"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 3, exceeds threshold of 2. Consider breaking this into smaller functions."})
     (lint! "(defn foo [x] (try (/ x 0) (catch Exception e 1) (catch Throwable t 2)))" config)))

  (testing "try with 1 catch has complexity 2"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 2, exceeds threshold of 1. Consider breaking this into smaller functions."})
     (lint! "(defn foo [x] (try (/ x 0) (catch Exception e 1)))"
            {:linters {:cyclomatic-complexity {:level :warning
                                               :threshold 1}}})))

  (testing "try with finally but no catch has complexity 1"
    (is (empty? (lint! "(defn foo [x] (try (/ x 0) (finally (println x))))" config)))))

(deftest threading-macros-test
  (testing "some-> with 3 forms has complexity 4"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 4, exceeds threshold of 3. Consider breaking this into smaller functions."})
     (lint! "(defn foo [x] (some-> x inc dec str))"
            {:linters {:cyclomatic-complexity {:level :warning
                                               :threshold 3}}})))

  (testing "some->> with 2 forms has complexity 3"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 3, exceeds threshold of 2. Consider breaking this into smaller functions."})
     (lint! "(defn foo [x] (some->> x inc dec))" config)))

  (testing "cond-> with 2 conditions has complexity 3"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 3, exceeds threshold of 2. Consider breaking this into smaller functions."})
     (lint! "(defn foo [x] (cond-> x true inc false dec))" config)))

  (testing "cond->> with 2 conditions has complexity 3"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 3, exceeds threshold of 2. Consider breaking this into smaller functions."})
     (lint! "(defn foo [x] (cond->> x true inc false dec))" config))))

(deftest recur-test
  (testing "recur does not add complexity (loop branching is counted via if/when)"
    (is (empty?
         (lint! "(defn foo [x] (if (< x 10) (recur (inc x)) x))"
                {:linters {:cyclomatic-complexity {:level :warning
                                                   :threshold 2}}})))))

(deftest complex-function-test
  (testing "realistic complex function exceeds threshold"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 7, exceeds threshold of 5. Consider breaking this into smaller functions."})
     (lint! "(defn process [x]
               (cond
                 (nil? x) nil
                 (neg? x) (if (even? x) :neg-even :neg-odd)
                 (zero? x) :zero
                 (pos? x) (if (even? x) :pos-even :pos-odd)
                 :else :unknown))"
            {:linters {:cyclomatic-complexity {:level :warning
                                               :threshold 5}}})))

  (testing "function just under threshold passes"
    (is (empty? (lint! "(defn foo [x] (if (< x 0) -1 (if (> x 0) 1 0)))"
                       {:linters {:cyclomatic-complexity {:level :warning
                                                          :threshold 3}}})))))

(deftest configuration-test
  (testing "linter disabled by default"
    (is (empty? (lint! "(defn foo [x] (if (< x 0) (if (> x 10) 1 2) 3))"))))

  (testing "linter respects level off"
    (is (empty? (lint! "(defn foo [x] (if (< x 0) (if (> x 10) 1 2) 3))"
                       {:linters {:cyclomatic-complexity {:level :off}}}))))

  (testing "linter respects custom threshold"
    (is (empty? (lint! "(defn foo [x] (if x 1 2))"
                       {:linters {:cyclomatic-complexity {:level :warning
                                                          :threshold 10}}}))))

  (testing "ignore metadata suppresses warning"
    (is (empty? (lint! "#_{:clj-kondo/ignore [:cyclomatic-complexity]}
                        (defn foo [x] (if (< x 0) (if (> x 10) 1 2) 3))"
                       config))))

  (testing "linter-specific ignore works"
    (is (empty? (lint! "^{:clj-kondo/ignore [:cyclomatic-complexity]}
                        (defn foo [x] (if (< x 0) (if (> x 10) 1 2) 3))"
                       config)))))

(deftest scope-test
  (testing "defn is measured"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1})
     (lint! "(defn foo [x] (if (< x 0) (if (> x 10) 1 2) 3))" config)))

  (testing "def with non-function is measured"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1})
     (lint! "(def foo (if true 1 2))"
            {:linters {:cyclomatic-complexity {:level :warning
                                               :threshold 1}}})))

  (testing "top-level let is measured"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1})
     (lint! "(let [x 1] (if x 2 3))"
            {:linters {:cyclomatic-complexity {:level :warning
                                               :threshold 1}}})))

  (testing "ns declaration has low complexity"
    (is (empty? (lint! "(ns foo.bar)" config)))))

(deftest clojurescript-test
  (testing "works with cljs.core forms"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 3, exceeds threshold of 2. Consider breaking this into smaller functions."})
     (lint! "(defn foo [x y] (if x (if y 1 2) 3))" config "--lang" "cljs"))))

(deftest nested-fn-test
  (testing "inner fn warns independently when outer is simple"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 14, :level :warning,
        :message "Cyclomatic complexity is 4, exceeds threshold of 2. Consider breaking this into smaller functions."})
     (lint! "(defn foo [] (fn [x] (if x (if x 1 2) (if x 3 4))))" config)))

  (testing "outer defn warns independently when inner fn is simple"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 4, exceeds threshold of 2. Consider breaking this into smaller functions."})
     (lint! "(defn foo [a b c] (and a b c (fn [x] (if x 1 2))))" config))))

(deftest comment-body-test
  (testing "branches inside (comment ...) do not count"
    (is (empty?
         (lint! "(defn foo [x] (comment (if x (if x 1 2) (if x 3 4))))" config)))))

(deftest while-test
  (testing "while contributes complexity"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 2, exceeds threshold of 1. Consider breaking this into smaller functions."})
     (lint! "(defn foo [x] (while x (println x)))"
            {:linters {:cyclomatic-complexity {:level :warning
                                               :threshold 1}}}))))

(deftest when-first-test
  (testing "when-first contributes complexity"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Cyclomatic complexity is 2, exceeds threshold of 1. Consider breaking this into smaller functions."})
     (lint! "(defn foo [xs] (when-first [x xs] x))"
            {:linters {:cyclomatic-complexity {:level :warning
                                               :threshold 1}}}))))
