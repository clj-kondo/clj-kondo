(ns clojure.test.are
  (:require #?(:clj [cljs.test :refer [are deftest testing]]
               :cljs [clojure.test :refer [are deftest testing]])))

(are [x y z] (= x y z)
  2 (+ 1 1) 2,
  4 (* 2 2) 4) ;; no unresolved symbols

;; see #1284
(deftest are-with-testing
  (are [f] (testing f
             (= "1" (f 1)))
    str
    pr-str))

(deftest are-with-testing2
  (are [f] (testing f
             (= "2" (f 1))
             (= "1" (f 1))))
  str
  pr-str)
