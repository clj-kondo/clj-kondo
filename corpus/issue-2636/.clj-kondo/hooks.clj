(ns hooks)

(defmacro if-bb [then else]
  (assert (= 2 ((requiring-resolve 'clojure.core/inc) 1)))
  `(if false ~then ~else))
