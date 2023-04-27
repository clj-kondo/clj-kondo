(ns clj-kondo.clara-test
  (:require
    [clj-kondo.test-utils :refer [lint!]]
    [clojure.java.io :as io]
    [clojure.test :as t :refer [deftest is testing]]
    [missing.test.assertions]))

(deftest clara-rules-test
  (is (= [{:row 14
           :col 11
           :level :on
           :message "Unresolved symbol: ?fact"}
          {:row 23
           :col 18
           :level :on
           :message "Unresolved symbol: foo-yes"}
          {:row 34
           :col 51
           :level :on
           :message "Unresolved symbol: foo-item"}
          {:row 41
           :col 20
           :level :on
           :message "Unresolved symbol: foo-value"}]
         (for [finding (lint! (io/file "corpus" "clara" "rules_test.clj")
                              {:linters
                               {:unresolved-symbol {:level :on}}})]
           (dissoc finding :file)))))
