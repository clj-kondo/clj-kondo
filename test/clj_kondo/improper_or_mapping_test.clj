(ns clj-kondo.improper-or-mapping-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps2] :rename {assert-submaps2 assert-submaps}]
   [clojure.test :refer [deftest is testing]]))

(deftest improper-or-mapping-test
  (testing "improper use of :or mapping in map destructuring"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 25, :level :warning, :message "Improper use of :or mapping: default value should not be an s-expression."})
     (lint! "(let [{:keys [x] :or {x (f1)}} {:x 1}] x)"
            {:linters {:improper-or-mapping {:level :warning}}})))
  (testing "multiple improper :or mappings"
    (assert-submaps
     '({:row 1, :col 27, :message "Improper use of :or mapping: default value should not be an s-expression."}
       {:row 1, :col 34, :message "Improper use of :or mapping: default value should not be an s-expression."})
     (lint! "(let [{:keys [x y] :or {x (f1) y (f2)}} {}] [x y])"
            {:linters {:improper-or-mapping {:level :warning}}})))
  (testing "nested map destructuring"
    (assert-submaps
     '({:row 1, :col 26, :message "Improper use of :or mapping: default value should not be an s-expression."})
     (lint! "(let [{{:keys [b] :or {b (f1)}} :a} {}] b)"
            {:linters {:improper-or-mapping {:level :warning}}})))
  (testing "vector destructuring (should not trigger)"
    (is (empty? (lint! "(let [[x y] [1 2]] x)"
                       {:linters {:improper-or-mapping {:level :warning}}}))))
  (testing "proper :or mapping (should not trigger)"
    (is (empty? (lint! "(let [{:keys [x] :or {x 1}} {:x 1}] x)"
                       {:linters {:improper-or-mapping {:level :warning}}})))))
