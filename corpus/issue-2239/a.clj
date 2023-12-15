(ns a)

(defmacro unknown-macro [name value]
  `(def ~name ~value))

(unknown-macro x 2)

(defmacro my-macro [& body]
  `(do ~@body))
