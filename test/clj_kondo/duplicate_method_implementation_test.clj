(ns clj-kondo.duplicate-method-implementation-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is]]))

(def config
  {:linters {:duplicate-method-implementation {:level :error}}})

(deftest duplicate-method-implementation-test
  (assert-submaps2
   '({:level :error :message "Duplicate method implementation: toString"})
   (lint! "(deftype T []
             Object
             (toString [this] \"one\")
             (toString [this] \"two\"))"
          config))
  (is (empty? (lint! "(deftype T []
                       Object
                       (toString [this] \"value\"))"
                     config))))
