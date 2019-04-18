(ns macros)

(defmacro foo [_])

(ns refer-all
  (:require [macros :refer-macros [foo]] :reload))

(foo) ;; caught
