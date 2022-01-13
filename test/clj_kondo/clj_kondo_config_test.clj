(ns clj-kondo.clj-kondo-config-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :refer [deftest testing is]]))

(deftest unexpected-linter-name-test
  (testing "Unexpected linter name"
    (assert-submaps
     '({:file ".clj-kondo/config.edn", :row 1, :col 12, :level :warning, :message "Unexpected linter name: :foo"})
     (lint! "{:linters {:foo 1}}" "--filename" ".clj-kondo/config.edn")))
  (testing "Linter config should go under :linters"
    (assert-submaps
     '({:file ".clj-kondo/config.edn", :row 1, :col 2, :level :warning, :message "Linter config should go under :linters"})
     (lint! "{:unresolved-symbol {}}" "--filename" ".clj-kondo/config.edn"))))

(deftest should-be-map-test
  (testing "Top level maps"
    (assert-submaps
     '({:file ".clj-kondo/config.edn", :row 1, :col 11, :level :warning, :message "Expected a map, but got: int"})
     (lint! "{:linters 1}" "--filename" ".clj-kondo/config.edn")))
  (testing "Linter config"
    (assert-submaps
     '({:file ".clj-kondo/config.edn", :row 1, :col 28, :level :warning, :message "Expected a map, but got: int"})
     (lint! "{:linters {:unused-binding 1}}" "--filename" ".clj-kondo/config.edn"))))
