(ns demo.macros)

(defmacro defvalue [sym value]
  `(def ~sym ~value))
