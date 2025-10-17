(ns clj-kondo.equals-nil-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is testing]]))

(deftest equals-nil-test
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 1, :level :warning, :message "Prefer (nil? x) over (= nil x)"})
   (lint! "(= nil x)" {:linters {:equals-nil {:level :warning}}}))
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 1, :level :warning, :message "Prefer (nil? x) over (= nil x)"})
   (lint! "(= x nil)" {:linters {:equals-nil {:level :warning}}}))
  (is (empty? (lint! "(= :foo :bar)" {:linters {:equals-nil {:level :warning}}})))
  (is (empty? (lint! "(= nil x y)" {:linters {:equals-nil {:level :warning}}})))
  (is (empty? (lint! "(nil? x)" {:linters {:equals-nil {:level :warning}}}))))

(deftest are-test
  (assert-submaps2 []
                   (lint! "(require '[clojure.test :refer [are]])
(are [exp time-style]
    (= exp (when time-style nil))
  :dude :dude
  nil nil)"
                          {:linters {:equals-nil {:level :warning}}})))
