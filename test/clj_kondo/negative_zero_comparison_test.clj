(ns clj-kondo.negative-zero-comparison-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is]]))

(def config
  {:linters {:negative-zero-comparison {:level :warning}}})

(deftest negative-zero-comparison-test
  (assert-submaps2
   '({:row 1
      :col 1
      :message "Use js/Object.is to compare against negative zero"})
   (lint! "(= x -0.0)" config "--lang" "cljs"))
  (is (empty? (lint! "(= x 0.0)" config "--lang" "cljs")))
  (is (empty? (lint! "(= x -0.0)" config "--lang" "clj"))))
