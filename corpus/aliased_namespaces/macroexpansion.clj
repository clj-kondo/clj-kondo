(ns macroexpansion
  (:require [clojure.core :as cc]))

(defn new-> [x f] (f x))

(new-> 1 inc)
