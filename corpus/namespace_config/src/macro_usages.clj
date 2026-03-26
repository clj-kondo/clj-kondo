(ns macro-usages
  (:require [macros :refer [with-foo]]
            [macros2 :as macros2]))

(with-foo [a 1]
  a) ;; no warning

(macros2/with-foo [a 1]
  a)
