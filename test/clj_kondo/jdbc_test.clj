(ns clj-kondo.jdbc-test
  (:require
   [clj-kondo.test-utils :refer [lint!]]
   [clojure.java.io :as io]
   [clojure.test :as t :refer [deftest is testing]]
   [missing.test.assertions]))

(deftest next-jdbc-test
  (is (empty? (lint! (io/file "corpus" "jdbc" "next_test.clj")
                     {:linters {:unresolved-symbol {:level :error}}}))))

(deftest clojure-java-jdbc-test
  (is (empty? (lint! (io/file "corpus" "jdbc" "cjj_test.clj")
                     {:linters {:unresolved-symbol {:level :error}}}))))
