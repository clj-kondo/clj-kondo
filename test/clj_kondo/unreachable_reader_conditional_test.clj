(ns clj-kondo.unreachable-reader-conditional-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps2]]
            [clojure.test :refer [deftest is testing]]))

(def ^:private msg "unreachable code. :default should be the last branch in a reader conditional")

(deftest unreachable-reader-conditional-test
  (let [config {:linters {:unreachable-code {:level :warning}}}]
    (testing ":default not last"
      (assert-submaps2
       [{:row 1 :col 4 :message msg}]
       (lint! "#?(:default 1 :clj 2)" config "--lang" "cljc"))
      (assert-submaps2
       [{:row 1 :col 12 :message msg}]
       (lint! "#?(:cljs 1 :default 2 :clj 3)" config "--lang" "cljc"))
      (assert-submaps2
       [{:row 1 :col 5 :message msg}]
       (lint! "#?@(:default [1] :clj [2])" config "--lang" "cljc"))
      (testing ":default last"
        (is (empty? (lint! "#?(:clj 1 :default 2)" config "--lang" "cljc")))
        (is (empty? (lint! "#?(:clj 1 :cljs 2 :default 3)" config "--lang" "cljc"))))

      (testing "disabled linter"
        (is (empty? (lint! "#?(:default 1 :clj 2)"
                           {:linters {:unreachable-code {:level :off}}}
                           "--lang" "cljc")))))))
