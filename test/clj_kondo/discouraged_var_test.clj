(ns clj-kondo.discouraged-var-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest discouraged-var-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 15, :level :warning, :message "Too slow"})
   (lint! "(defn foo [x] (satisfies? Datafy x))"
          '{:linters  {:discouraged-var {clojure.core/satisfies? {:message "Too slow"}}}}))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 15, :level :warning, :message "Too slow"})
   (lint! "(defn foo [x] (satisfies? Datafy x))"
          '{:ns-groups [{:pattern "(cljs|clojure).core" :name core}]
            :linters  {:discouraged-var {core/satisfies? {:message "Too slow"}}}}))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 34, :level :warning, :message "Closed source"})
   (lint! "(require '[closed.source :as s]) (s/fn)"
          '{:linters  {:discouraged-var {closed.source/fn {:message "Closed source"}}}}))
  (assert-submaps
   '()
   (lint! "(require '[closed.source :as s]) (comment (s/fn))"
                 '{:linters  {:discouraged-var {closed.source/fn {:message "Closed source"}}}
                   :config-in-comment
                   {:linters {:discouraged-var {:level :off}}}}))
  (assert-submaps
    '()
    (lint!
      (str "(ns foo {:clj-kondo/config {:linters {:discouraged-var {:level :off}}}})\n"
           "(satisfies? Datafy 5)")
      '{:linters  {:discouraged-var {clojure.core/satisfies? {:message "Too slow"}}}})))
