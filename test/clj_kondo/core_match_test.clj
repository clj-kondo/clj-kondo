(ns clj-kondo.core-match-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps lint!]]
   [clojure.test :as t :refer [deftest is testing]]
   [missing.test.assertions]))

(deftest core-match-test
  (is (empty? (lint! "(require '[clojure.core.match :refer [match]])

(match [1 2 3]
  [1 2 _] :foo)
"
                     {:linters {:unresolved-symbol {:level :error}}})))
  (assert-submaps
   '({:file "<stdin>", :row 3, :col 8, :level :warning, :message "unused binding a"})
   (lint! "(require '[clojure.core.match :refer [match]])
(match [1 2 3]
  [1 2 a] :foo)
" {:linters {:unused-binding {:level :warning}
             :unresolved-symbol {:level :error}}})))

