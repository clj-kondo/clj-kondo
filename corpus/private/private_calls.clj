(ns private.private-calls
  (:require [private.private-defs :as x :refer [private]]))

(private 1 2 3)
(x/private-by-meta)
(map private [])
(map #'private.private-defs/private []) ;; <- should not be reported
