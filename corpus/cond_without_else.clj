(ns cond-without-else1
  (:refer-clojure :exclude [cond])
  (:require [clojure.core :as c]))

(def n (rand-int 10))

(c/cond
  (neg? n) "negative"
  (pos? n) "positive")

(ns cond-without-else2)
(def n (rand-int 10))

(cond
  (neg? n) "negative"
  (pos? n) "positive")
