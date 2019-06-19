(ns defmulti
  (:require [integrant.core :as ig]))

(defmulti greeting
  (fn [x] (x "language")))

(defmethod greetingx "English" [x y]
  x)

(defmethod ig/pre-init-spec :my/key [_] ::args)

(defmulti xyz (fn ([x _] x) ([x _ _] x)))
(defmethod xyz "z" ([x y] "z") ([x y z] "z"))
