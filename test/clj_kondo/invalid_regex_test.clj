(ns clj-kondo.invalid-regex-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is]]))

(def config
  {:linters {:invalid-regex {:level :error}}})

(deftest invalid-regex-test
  (assert-submaps2
   '({:row 1 :col 13 :level :error :message #"Invalid regex"})
   (lint! "(re-pattern \"[\")" config))
  (is (empty? (lint! "(re-pattern \"[a-z]\")" config)))
  (is (empty? (lint! "(re-pattern pattern)" config)))
  (is (empty? (lint! "(re-pattern \"[\")" config "--lang" "cljs"))))
