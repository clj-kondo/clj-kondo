(ns use)

(ns foo
  (:use [clojure.string]))

(ns bar
  (:use [clojure.string :only [join]]))

(ns baz)
(require '[clojure.string :refer :all])
join

(ns quuz
  (:require [clojure.string :refer :all]))

(ns foo2)
;; TODO:
(use 'clojure.set)
