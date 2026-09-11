(ns clj-kondo.regex-spaces-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is]]))

(def config
  {:linters {:regex-spaces {:level :warning}}})

(deftest regex-spaces-test
  (assert-submaps2
   '({:row 1
      :col 1
      :message "Use a quantifier instead of multiple spaces in a regex"})
   (lint! "#\"foo  bar\"" config))
  (is (empty? (lint! "#\"foo {2}bar\"" config)))
  (is (empty? (lint! "#\"foo bar\"" config)))
  (is (empty? (lint! "#\"[a-z  A-Z]\"" config)))
  (is (empty? (lint! "#\"(?x)foo  bar\"" config)))
  (is (empty? (lint! "#\"foo  bar\""
                     {:linters {:regex-spaces {:level :off}}}))))
