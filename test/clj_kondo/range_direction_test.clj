(ns clj-kondo.range-direction-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is]]))

(def config
  {:linters {:range-direction {:level :warning}}})

(deftest range-direction-test
  (assert-submaps2
   '({:row 1
      :col 13
      :message "Range step moves away from the end value"})
   (lint! "(range 0 10 -1)" config))
  (assert-submaps2
   '({:row 1
      :col 13
      :message "Range step moves away from the end value"})
   (lint! "(range 10 0 1)" config))
  (assert-submaps2
   '({:row 1
      :col 11
      :message "Range step moves away from the end value"})
   (lint! "(range 10 0)" config))
  (is (empty? (lint! "(range 0 10 1) (range 10 0 -1)" config)))
  (is (empty? (lint! "(range start end step)" config)))
  (is (empty? (lint! "(range 0 10 -1)"
                     {:linters {:range-direction {:level :off}}}))))
