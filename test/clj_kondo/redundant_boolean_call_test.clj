(ns clj-kondo.redundant-boolean-call-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is]]))

(def config
  {:linters {:redundant-boolean-call {:level :warning}}})

(deftest redundant-boolean-call-test
  (assert-submaps2
   '({:row 1
      :col 5
      :message "Boolean call is redundant in condition position"})
   (lint! "(if (boolean x) 1 2)" config))
  (assert-submaps2
   '({:row 1
      :col 7
      :message "Boolean call is redundant in condition position"})
   (lint! "(when (boolean (seq xs)) 1)" config))
  (is (empty? (lint! "(boolean x)" config)))
  (is (empty? (lint! "(ns foo)
                       (defmacro m [& xs])
                       (m (boolean x) 1 2)"
                     {:linters {:redundant-boolean-call {:level :warning}}
                      :lint-as {'foo/m 'clojure.core/if}})))
  (is (empty? (lint! "(if (boolean x) 1 2)"
                     {:linters {:redundant-boolean-call {:level :off}}}))))
