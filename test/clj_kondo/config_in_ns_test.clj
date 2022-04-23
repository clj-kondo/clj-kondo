(ns clj-kondo.config-in-ns-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest config-in-ns-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 23, :level :error, :message "Expected: number, received: keyword."})
   (lint! "(ns dude2) x y z (inc :foo)"
          '{:ns-groups [{:pattern "dude.*" :name dude-group}]
            :config-in-ns {dude-group {:linters {:unresolved-symbol {:level :off}}}}
            :linters  {:unresolved-symbol {:level :error}
                       :type-mismatch {:level :error}}})))
