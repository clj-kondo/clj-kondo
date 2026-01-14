(ns clj-kondo.reader-conditional-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps2]]
            [clojure.test :refer [deftest is testing]]))

(deftest reader-conditional-default-not-last-test
  (let [config {:linters {:reader-conditional-default-not-last {:level :warning}}}]
    (testing ":default not last"
      (assert-submaps2
       [{:row 1 :col 4 :message ":default should be the last branch in a reader conditional"}]
       (lint! "#?(:default 1 :clj 2)" config "--lang" "cljc"))
      (assert-submaps2
       [{:row 1 :col 12 :message ":default should be the last branch in a reader conditional"}]
       (lint! "#?(:cljs 1 :default 2 :clj 3)" config "--lang" "cljc"))
      (assert-submaps2
       [{:row 1 :col 5 :message ":default should be the last branch in a reader conditional"}]
       (lint! "#?@(:default [1] :clj [2])" config "--lang" "cljc")))

    (testing ":default last"
      (is (empty? (lint! "#?(:clj 1 :default 2)" config "--lang" "cljc")))
      (is (empty? (lint! "#?(:clj 1 :cljs 2 :default 3)" config "--lang" "cljc"))))

    (testing "disabled linter"
      (is (empty? (lint! "#?(:default 1 :clj 2)"
                         {:linters {:reader-conditional-default-not-last {:level :off}}}
                         "--lang" "cljc"))))))
