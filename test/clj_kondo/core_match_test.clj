(ns clj-kondo.core-match-test
  (:require
   [clj-kondo.test-utils :refer [lint!]]
   [clojure.java.io :as io]
   [clojure.test :as t :refer [deftest is testing]]
   [missing.test.assertions]))

(deftest core-match-test
  (prn (lint! "(require '[clojure.core.match :refer [match]])

(match [1 2 3] x
 [1 2 _] :foo)
"
              {:linters {:unresolved-symbol {:level :error}}})))

