(ns spec.alpha
  (:refer-clojure :exclude [def])
  (:require-macros [spec.alpha :as m]))

(def x 1) ;; no arity error, although spec.alpha/def lives in a same-named ns
(m/def foo ::foo)
