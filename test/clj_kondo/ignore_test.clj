(ns clj-kondo.ignore-test
  (:require  [clojure.test :as t :refer [deftest is testing]]
             [clj-kondo.test-utils :refer [lint! assert-submaps]]))

(deftest ignore-test
  (is (empty? (lint! "#_:clj-kondo/ignore (inc :foo)"
                     {:linters {:type-mismatch {:level :warning}}}))))


