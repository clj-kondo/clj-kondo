(ns b (:require [a :refer :all])
      (:import [a Foo]))

(defn dude [^Foo x]
  (.-y x))

