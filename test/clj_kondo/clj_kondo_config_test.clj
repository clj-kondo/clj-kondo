(ns clj-kondo.clj-kondo-config-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :refer [deftest testing is]]))

(deftest unexpected-linter-name-test
  (assert-submaps
   '({:file ".clj-kondo/config.edn", :row 1, :col 12, :level :warning, :message "Unexpected linter name: :foo"})
   (lint! "{:linters {:foo 1}}" "--filename" ".clj-kondo/config.edn"))
  (assert-submaps
   '({:file ".clj-kondo/config.edn", :row 1, :col 2, :level :warning, :message "Linter config should go under :linters"})
   (lint! "{:unresolved-symbol {}}" "--filename" ".clj-kondo/config.edn")))
