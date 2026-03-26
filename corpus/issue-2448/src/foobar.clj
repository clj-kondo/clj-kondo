(ns foobar
  (:require [ductile.util :as util]))

(util/cond+
 false 1
 (and true
      :let [x 2]
      (= 3 x)) 4
 (and true
      :let [x 3]
      (= 3 x)) x)

(or (or 1 2) 2)
