(ns b
  (:require
   [a :refer [my-macro]]))

(defn looses-name []
  (my-macro (println a/x)))
