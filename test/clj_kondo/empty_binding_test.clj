(ns clj-kondo.empty-binding-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is]]))

(def config
  {:linters {:empty-binding {:level :warning}}})

(deftest empty-binding-test
  (assert-submaps2
   '({:row 1 :col 7 :message "Empty destructuring form binds no values"})
   (lint! "(let [{} x] x)" config))
  (assert-submaps2
   '({:row 1 :col 7 :message "Empty destructuring form binds no values"})
   (lint! "(let [[] x] x)" config))
  (is (empty? (lint! "(fn []) (let [{:keys [x]} m] x)" config))))
