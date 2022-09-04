(ns clj-kondo.config-in-ns-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps lint!]]
   [clojure.java.io :as io]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest config-in-ns-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 23, :level :error, :message "Expected: number, received: keyword."})
   (lint! "(ns dude2) x y z (inc :foo)"
          '{:ns-groups [{:pattern "dude.*" :name dude-group}]
            :config-in-ns {dude-group {:linters {:unresolved-symbol {:level :off}}}}
            :linters  {:unresolved-symbol {:level :error}
                       :type-mismatch {:level :error}}})))

(deftest config-in-ns-override-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 25, :level :warning, :message "No"})
   (lint! "(ns my.namespace) x y z (assoc nil :foo :bar)"
          '{:ns-groups [{:pattern "my.*" :name mine}]
            :config-in-ns {mine {:linters {:discouraged-var {clojure.core/assoc {:message "No"}}}}
                           my.namespace {:linters {:unresolved-symbol {:level :off}}}}})))

(deftest config-in-ns-file-pattern-test
  (is (empty?
       (lint! (io/file "corpus" "config_in_ns" "my_custom_ns.clj")
              '{:ns-groups [{:filename-pattern ".*config_in_ns.*" :name mine}]
                :config-in-ns {mine {:linters {:unused-private-var {:level :off}}}}}))))
