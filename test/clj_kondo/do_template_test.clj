(ns clj-kondo.do-template-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is testing]]))

(deftest all-okay-test
  (is (empty?
       (lint!
        "(ns foo.bar (:require [clojure.template]))
        (clojure.template/do-template [a b]
        (prn a b)
        1 2
        3 4)"
        {:linters {:do-template {:level :warning}}}))))

(deftest empty-args-test
  (assert-submaps2
   '({:file "<stdin>", :row 2, :col 5, :level :warning,
      :message "No args defined. Expected at least 1."})
   (lint!
    "(ns foo.bar (:require [clojure.template]))
    (clojure.template/do-template []
    (prn)
    1 2)"
    {:linters {:do-template {:level :warning}}})))

(deftest empty-values-test
  (assert-submaps2
   '({:file "<stdin>", :row 2, :col 5, :level :warning,
      :message "No values provided. Expected: multiple of 2."})
   (lint!
    "(ns foo.bar (:require [clojure.template]))
    (clojure.template/do-template [a b]
    (prn))"
    {:linters {:do-template {:level :warning}}})))

(deftest mismatching-values-test
  (assert-submaps2
   '({:file "<stdin>", :row 2, :col 5, :level :warning,
      :message "Incorrect number of values provided. Expected: multiple of 2."})
   (lint!
    "(ns foo.bar (:require [clojure.template]))
    (clojure.template/do-template [a b]
    (prn a b)
    1 2 3)"
    {:linters {:do-template {:level :warning}}})))
