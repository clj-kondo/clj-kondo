(ns clj-kondo.equals-false-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is testing]]))

(deftest equals-false-test
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 1, :level :warning, :message "Prefer (false? x) over (= false x)"})
   (lint! "(= false 1)" {:linters {:equals-false {:level :warning}}}))
  (is (empty? (lint! "(= false 1 1)" {:linters {:equals-false {:level :warning}}}))))
