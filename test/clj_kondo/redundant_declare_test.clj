(ns clj-kondo.redundant-declare-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps2]]
   [clojure.test :refer [deftest is testing]]))

(deftest redundant-declare-test
  (testing "warns when declare is used after var is already defined"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 15, :level :warning,
        :message "Redundant declare: foo"})
     (lint! "(defn foo []) (declare foo)"))

    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 13, :level :warning,
        :message "Redundant declare: foo"})
     (lint! "(def foo 1) (declare foo)")))

  (testing "no warning for normal forward declare pattern"
    (is (empty? (lint! "(declare foo) (defn foo [])"))))

  (testing "no warning for idempotent declare"
    (is (empty? (lint! "(declare foo) (declare foo)"))))

  (testing "no warning when linter is disabled"
    (is (empty? (lint! "(defn foo []) (declare foo)"
                       {:linters {:redundant-declare {:level :off}}}))))

  (testing "no warning when ignored inline"
    (is (empty? (lint! "(defn foo []) ^{:clj-kondo/ignore [:redundant-declare]} (declare foo)"))))

  (testing "multiple declares after def"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 13, :level :warning,
        :message "Redundant declare: foo"}
       {:file "<stdin>", :row 1, :col 39, :level :warning,
        :message "Redundant declare: bar"})
     (lint! "(def foo 1) (declare foo) (def bar 2) (declare bar)")))

  (testing "declare multiple vars where one is defined"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 13, :level :warning,
        :message "Redundant declare: foo"})
     (lint! "(def foo 1) (declare foo bar)"))))
