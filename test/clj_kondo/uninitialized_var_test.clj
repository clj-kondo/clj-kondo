(ns clj-kondo.uninitialized-var-test
  (:require
   [clj-kondo.test-utils :as tu :refer [lint! assert-submaps2]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest uninitialized-var-test
  (assert-submaps2
   [{:row 1,
     :col 1,
     :level :warning,
     :message "Uninitialized var"}]
   (lint! "(def x)" {:linters {:uninitialized-var {:level :warning}}})))

