(ns feature-syntax
  (:require [clojure.test :as t]))

(t/deftest foo-test
  #?(:clj
     (t/testing "foo"
       (prn :x))
     (t/testing "bar"
       (prn :y))))
