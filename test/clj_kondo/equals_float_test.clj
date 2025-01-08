(ns clj-kondo.equals-float-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is testing]]))

(deftest equals-float-test
  (prn (lint! "(fn [x] (= 0.3 x))" {:linters {:equals-float {:level :warning}}})))
