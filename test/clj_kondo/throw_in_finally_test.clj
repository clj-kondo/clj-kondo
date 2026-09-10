(ns clj-kondo.throw-in-finally-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is]]))

(def config
  {:linters {:throw-in-finally {:level :warning}}})

(deftest throw-in-finally-test
  (assert-submaps2
   '({:row 1
      :message "Throw in finally replaces the pending value or exception"})
   (lint! "(try (work) (finally (throw failure)))" config))
  (assert-submaps2
   '({:row 1
      :message "Throw in finally replaces the pending value or exception"})
   (lint! "(try (work) (finally (when failed? (throw failure))))" config))
  (is (empty? (lint! "(try (work) (finally (fn [] (throw failure))))"
                     config))))
