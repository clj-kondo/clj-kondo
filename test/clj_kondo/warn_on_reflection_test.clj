(ns clj-kondo.warn-on-reflection-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest warn-on-reflection-test
  (assert-submaps
   []
   (lint! "(ns foo) (defn foo [])"
          '{:linters {:warn-on-reflection {:level :warning}}}))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 1, :level :warning, :message "Var *warn-on-reflection* is not set in this namespace."})
   (lint! "(ns foo) (defn foo [])"
          '{:linters {:warn-on-reflection {:level :warning
                                           :warn-only-on-interop false}}}))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 23, :level :warning, :message "Var *warn-on-reflection* is not set in this namespace."})
   (lint! "(ns foo) (defn foo [] (.foo ))"
          '{:linters {:warn-on-reflection {:level :warning}}}))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 23, :level :warning, :message "Var *warn-on-reflection* is not set in this namespace."})
   (lint! "(ns foo) (defn foo [] (Thread/sleep 100))"
          '{:linters {:warn-on-reflection {:level :warning}}})))
