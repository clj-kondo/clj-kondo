(ns clj-kondo.cljs-unsafe-integer-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is]]))

(def config
  {:linters {:cljs-unsafe-integer {:level :warning}}})

(deftest cljs-unsafe-integer-test
  (assert-submaps2
   '({:row 1
      :col 1
      :message "Integer cannot be represented exactly in ClojureScript"})
   (lint! "9007199254740993" config "--lang" "cljs"))
  (is (empty? (lint! "9007199254740991" config "--lang" "cljs")))
  (is (empty? (lint! "9007199254740993" config "--lang" "clj"))))
