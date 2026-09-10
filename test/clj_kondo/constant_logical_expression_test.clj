(ns clj-kondo.constant-logical-expression-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is]]))

(def config
  {:linters {:constant-logical-expression {:level :warning}}})

(deftest constant-logical-expression-test
  (assert-submaps2
   '({:row 1 :col 5 :message "Later logical operands are unreachable"})
   (lint! "(or true (work))" config))
  (assert-submaps2
   '({:row 1 :col 6 :message "Later logical operands are unreachable"})
   (lint! "(and false (work))" config))
  (is (empty? (lint! "(or false (work)) (and true (work))" config)))
  (is (empty? (lint! "(or true (work))"
                     {:linters {:constant-logical-expression
                                {:level :off}}}))))
