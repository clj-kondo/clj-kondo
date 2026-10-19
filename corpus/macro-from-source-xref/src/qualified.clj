(ns qualified
  (:require [helpers]))

(defmacro qlet
  "Marker macro that calls a helper by its fully qualified name at expand time."
  {:clj-kondo/macroexpand-hook true}
  [bindings & body]
  {:pre [(helpers/binding-vec? bindings)]}
  `(let ~bindings ~@body))

(qlet [a 12 b 34] (+ a b))
