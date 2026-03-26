(ns hooks)

(defmacro if-bb [then else]
  `(if false ~then ~else))
