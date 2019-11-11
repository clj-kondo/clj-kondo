(ns clojure.test.are
  (:require #?(:clj [cljs.test :refer [are]]
               :cljs [clojure.test :refer [are]])))

(are [x y z] (= x y z)
  2 (+ 1 1) 2,
  4 (* 2 2) 4) ;; no unresolved symbols

(are [x y a] (= x y z) ;; unused binding a, unresolved symbol z
  2 (+ 1 1) 2,
  4 (* 2 2) 4)
