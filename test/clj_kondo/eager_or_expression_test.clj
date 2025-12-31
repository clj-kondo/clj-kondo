(ns clj-kondo.eager-or-expression-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps2] :rename {assert-submaps2 assert-submaps}]
   [clojure.test :refer [deftest is testing]]))

(deftest eager-or-expression-test
  (testing "call in :or mapping in map destructuring"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 25, :level :warning, :message "Default value in :or is an s-expression and will be eagerly evaluated."})
     (lint! "(let [{:keys [x] :or {x (f1)}} {:x 1}] x)"
            {:linters {:eager-or-expression {:level :warning}}})))
  (testing "multiple calls in :or mappings"
    (assert-submaps
     '({:row 1
        :col 27
        :message "Default value in :or is an s-expression and will be eagerly evaluated."}
       {:row 1
        :col 34
        :message "Default value in :or is an s-expression and will be eagerly evaluated."})
     (lint! "(let [{:keys [x y] :or {x (f1) y (f2)}} {}] [x y])"
            {:linters {:eager-or-expression {:level :warning}}})))
  (testing "nested map destructuring"
    (assert-submaps
     '({:row 1
        :col 26
        :message "Default value in :or is an s-expression and will be eagerly evaluated."})
     (lint! "(let [{{:keys [b] :or {b (f1)}} :a} {}] b)"
            {:linters {:eager-or-expression {:level :warning}}})))
  (testing "vector destructuring (should not trigger)"
    (is (empty? (lint! "(let [[x y] [1 2]] x)"
                       {:linters {:eager-or-expression {:level :warning}}}))))
  (testing "proper :or mapping (should not trigger)"
    (is (empty? (lint! "(let [{:keys [x] :or {x 1}} {:x 1}] x)"
                       {:linters {:eager-or-expression {:level :warning}}}))))
  (testing "empty list in :or (should not trigger)"
    (is (empty? (lint! "(let [{:keys [x] :or {x ()}} {:x 1}] x)"
                       {:linters {:eager-or-expression {:level :warning}}}))))
  (testing "collection literal with call in :or"
    (assert-submaps
     '({:row 1
        :col 29
        :message "Default value in :or is an s-expression and will be eagerly evaluated."})
     (lint! "(let [{:keys [x] :or {x {:y (f1)}}} {}] x)"
            {:linters {:eager-or-expression {:level :warning}}}))))
