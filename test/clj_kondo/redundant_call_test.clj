(ns clj-kondo.redundant-call-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :refer [deftest is testing]]))

(deftest redundant-call-test
  (doseq [sym ['-> '->> 'cond-> 'cond->> 'some-> 'some->> 'partial 'comp 'merge]]
    (is (empty? (lint! (format "(%s 1 inc)" sym))))
    (assert-submaps
      [{:level :warning :message (format "Single arg use of %s always returns the arg itself" sym)}]
      (lint! (format "(%s 1)" sym)))
    (assert-submaps
      [{:level :warning :message (format "Single arg use of %s always returns the arg itself" sym)}
       {:level :warning :message (format "Single arg use of %s always returns the arg itself" sym)}]
      (lint! (format "(%s (%s 1))" sym sym)))))
