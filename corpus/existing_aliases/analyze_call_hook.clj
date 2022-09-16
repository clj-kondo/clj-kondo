(ns analyze-call-hook
  (:require [clojure.core :as cc]))

(defn new-> [x f] (f x))

(new-> 1 inc)
