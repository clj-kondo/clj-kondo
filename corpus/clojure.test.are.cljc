(ns clojure.test.are
  (:require #?(:clj [clojure.test :refer [are]]
               :cljs [clojure.test :refer [are]])))

(are [x y] (= x y)
  2 (+ 1 1)
  4 (* 2 2))
