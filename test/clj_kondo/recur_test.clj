(ns clj-kondo.recur-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :as t :refer [deftest is testing]]))

(def linter-config
  {:linters
   {:unexpected-recur {:level :error}}})

(deftest unexpected-recur
  (testing "all linters with def"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 1, :level :error, :message "Unexpected usage of recur."})
     (lint! "(recur)" linter-config))))

