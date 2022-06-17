(ns clj-kondo.discouraged-ns-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest discouraged-ns-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 12, :level :warning, :message "deprecated namespace"}
     {:file "<stdin>", :row 1, :col 37, :level :warning, :message "deprecated namespace"}
     {:file "<stdin>", :row 1, :col 56, :level :warning, :message "deprecated namespace"}
     {:file "<stdin>", :row 1, :col 76, :level :warning, :message "deprecated namespace"})
   (lint! "(require '[closed.source :as s]) (#'closed.source/foo) (closed.source/bar) (s/baz)"
          '{:linters  {:discouraged-ns {closed.source {:message "deprecated namespace"}}}}))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 1, :level :warning, :message "deprecated namespace"}
     {:file "<stdin>", :row 1, :col 15, :level :warning, :message "deprecated namespace"})
   (lint! "(defn foo [x] (satisfies? Datafy x))"
          '{:ns-groups [{:pattern "(cljs|clojure).core" :name core}]
            :linters  {:discouraged-ns {core {:message "deprecated namespace"}}}}))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 79, :level :warning, :message "namespace foo is required but never used"})
   (lint! "(ns foo) (defn x []) (ns bar (:require [foo :as f])) (f/x) (ns baz (:require [foo :as f]))"
          '{:linters  {:discouraged-ns {foo {:from #{baz}
                                             :message "deprecated namespace"}}}})))
