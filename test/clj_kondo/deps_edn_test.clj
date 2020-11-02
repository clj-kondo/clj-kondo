(ns clj-kondo.deps-edn-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps]]
            [clojure.test :refer [deftest testing is]]))

(deftest qualified-lib-test
  (let [deps-edn '{:deps {clj-kondo {:mvn/version "2020.10.10"}}
                   :aliases {:foo {:extra-deps {clj-kondo {:mvn/version "2020.10.10"
                                                           :exclusions [cheshire]}}}}}]
    (assert-submaps
     '({:file "deps.edn", :row 1, :col 9,
        :level :warning, :message "Libs must be qualified, change clj-kondo => clj-kondo/clj-kondo"}
       {:file "deps.edn", :row 1, :col 79,
        :level :warning, :message "Libs must be qualified, change clj-kondo => clj-kondo/clj-kondo"}
       {:file "deps.edn", :row 1, :col 130,
        :level :warning, :message "Libs must be qualified, change cheshire => cheshire/cheshire"})
     (lint! (str deps-edn)
                "--filename" "deps.edn"))))
