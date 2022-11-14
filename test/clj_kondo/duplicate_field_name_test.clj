(ns clj-kondo.duplicate-field-name-test
  (:require [clj-kondo.test-utils :refer [assert-submaps lint!]]
            [clojure.test :refer [deftest is testing]]
            [missing.test.assertions]))

(deftest duplicate-field-name-test
  (testing "deftype"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 13, :level :error, :message "Duplicate field name: field"}
       {:file "<stdin>", :row 1, :col 33, :level :error, :message "Duplicate field name: field"})
     (lint! "(deftype T [field another-field field])")))

  (testing "defrecord"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 15, :level :error, :message "Duplicate field name: field"}
       {:file "<stdin>", :row 1, :col 35, :level :error, :message "Duplicate field name: field"})
     (lint! "(defrecord R [field another-field field])"))))
