(ns clj-kondo.equals-float-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is testing]]))

(deftest equals-float-test
  (assert-submaps2
   [{:file "<stdin>", :row 1, :col 9, :level :warning, :message "Equality comparison of floating point numbers"}]
   (lint! "(fn [x] (= 0.3 x))" {:linters {:equals-float {:level :warning}
                                          :type-mismatch {:level :warning}}})))
