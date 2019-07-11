(ns cljs-self-require
  #?(:cljs (:require-macros [cljs-self-require :refer [foo]])))

(defmacro foo [x]
  `(do (println ~x) (println ~x)))
