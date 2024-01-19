(ns hooks.a)

(defmacro my-defrecord [& body]
  `(defrecord ~@body))
