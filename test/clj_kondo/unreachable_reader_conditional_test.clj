(ns clj-kondo.unreachable-reader-conditional-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps2]]
            [clojure.test :refer [deftest is testing]]))

(def ^:private msg "Unreachable code: default reader conditional branch should go last")

(deftest unreachable-reader-conditional-test
  (testing ":default not last"
    (assert-submaps2
     [{:row 1 :col 4 :level :warning :message msg}]
     (lint! "#?(:default 1 :clj 2)" "--lang" "cljc"))
    (assert-submaps2
     [{:row 1 :col 12 :level :warning :message msg}]
     (lint! "#?(:cljs 1 :default 2 :clj 3)" "--lang" "cljc"))
    (assert-submaps2
     [{:row 1 :col 5 :level :warning :message msg}]
     (lint! "#?@(:default [1] :clj [2])" "--lang" "cljc"))

    (testing ":default last"
      (is (empty? (lint! "#?(:clj 1 :default 2)" "--lang" "cljc")))
      (is (empty? (lint! "#?(:clj 1 :cljs 2 :default 3)" "--lang" "cljc"))))

    (testing "disabled linter"
      (is (empty? (lint! "#?(:default 1 :clj 2)"
                         {:linters {:unreachable-code {:level :off}}}
                         "--lang" "cljc"))))))
