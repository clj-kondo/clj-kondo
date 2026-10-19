(ns wrappers)

(defmacro hook-wrap
  {:clj-kondo/macroexpand-hook true}
  [& body]
  `(do ~@body))

(defmacro plain-wrap
  [& body]
  `(do ~@body))
