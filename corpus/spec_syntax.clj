(ns spec-syntax
  (:require [clojure.spec.alpha :as s]))

(s/fdef my-inc ;; symbol isn't reported as unresolved
  :args (s/cat :x int?))

(s/fdef 1 :args) ;; expected symbol, missing value for :args

(s/fdef foo :xargs (s/cat :x int?))

(defn my-inc [x]
  (+ 1 x))







