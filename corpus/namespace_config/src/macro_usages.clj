(ns macro-usages
  (:require [macros :refer [with-foo]]))

(with-foo [a 1]
  a) ;; no warning
