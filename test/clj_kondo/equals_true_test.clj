(ns clj-kondo.equals-true-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is testing]]))

(deftest equals-true-test
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 1, :level :warning, :message "Prefer (true? x) over (= true x)"})
   (lint! "(= true 1)" {:linters {:equals-true {:level :warning}}}))
  (is (empty? (lint! "(= true 1 1)" {:linters {:equals-true {:level :warning}}}))))
