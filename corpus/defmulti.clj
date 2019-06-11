(ns defmulti
  (:require [integrant.core :as ig]))

(defmulti greeting
  (fn [x] (x "language")))

(defmethod greetingx "English" [x y]
  x)

(defmethod ig/pre-init-spec :my/key [_] ::args)
