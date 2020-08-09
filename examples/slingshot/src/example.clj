(ns math.expression
  (:require [slingshot.slingshot :refer [throw+ try+]]))

(defn read-file [file]
  (try+
   (prn file)
   (catch [:type :tensor.parse/bad-tree] {:keys [tree hint]}
     (prn tree hint)
     (throw+))
   (catch Object _
     (throw+))))
