(ns clj-kondo.clara-test
  (:require
   [clj-kondo.test-utils :refer [lint!]]
   [clojure.java.io :as io]
   [clojure.test :as t :refer [deftest is testing]]
   [missing.test.assertions]))

(deftest clara-rules-no-errors-test
  (is (empty? (lint! (io/file "corpus" "clara" "rules_test.clj")
                     {:linters
                      {:unresolved-symbol {:level :on}}}))))
