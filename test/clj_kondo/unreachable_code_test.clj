(ns clj-kondo.unreachable-code-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is]]))

(def config
  {:linters {:unreachable-code {:level :warning}}})

(deftest unreachable-after-throw-test
  (assert-submaps2
   '({:row 1 :col 15 :message "Unreachable code"})
   (lint! "(do (throw e) (cleanup))" config))
  (is (empty? (lint! "(if failed? (throw e) (cleanup))" config))))
