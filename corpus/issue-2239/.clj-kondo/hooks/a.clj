(ns hooks.a)

(defmacro my-macro [& body]
  `(do ~@body))
