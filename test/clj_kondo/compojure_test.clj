(ns clj-kondo.compojure-test
  (:require
   [clj-kondo.test-utils :refer [lint!]]
   [clojure.java.io :as io]
   [clojure.test :as t :refer [deftest is testing]]
   [missing.test.assertions]))

(deftest compojure-test
  (is (empty? (lint! (io/file "corpus" "compojure" "core_test.clj")))))
