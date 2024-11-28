(ns clj-kondo.condition-always-true-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps2]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest condition-always-true-test
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 20, :level :warning, :message "Condition always true"}
     {:file "<stdin>",
      :row 1,
      :col 35,
      :level :warning,
      :message "Condition always true"})
   (lint! "(defn foo [x] [(if inc x 2) (when inc 2)])"
          '{:linters {:condition-always-true {:level :warning}}}))
  (is (empty?
       (lint! "(defn foo [x] (if x inc 2))
               (defn bar [x] (if x 2 inc))"
              '{:linters {:condition-always-true {:level :warning}}})))
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 7,
     :level :warning,
     :message "Condition always true"}]
   (lint! "(when #'inc 2)"
          '{:linters {:condition-always-true {:level :warning}}}))
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 9,
     :level :warning,
     :message "Condition always true"}]
   (lint! "(if-not odd? 1 2)"
          '{:linters {:condition-always-true {:level :warning}}}))
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 12,
     :level :warning,
     :message "Condition always true"}]
   (lint! "(if-let [a odd?] 1 2)"
          '{:linters {:condition-always-true {:level :warning}}})))
