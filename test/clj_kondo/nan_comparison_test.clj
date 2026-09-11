(ns clj-kondo.nan-comparison-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is]]))

(def config
  {:linters {:nan-comparison {:level :warning}}})

(deftest nan-comparison-test
  (doseq [code ["(= x ##NaN)" "(not= ##NaN x)" "(== x ##NaN)"]]
    (assert-submaps2
     '({:row 1 :col 1 :message "NaN cannot be compared"})
     (lint! code config)))
  (is (empty? (lint! "(Double/isNaN x)" config)))
  (is (empty? (lint! "(= x 1.0)" config))))
