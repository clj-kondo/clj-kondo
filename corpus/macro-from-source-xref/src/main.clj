(ns main
  (:require [helpers :as h]))

(defmacro mylet
  "Marker macro that calls a helper from another namespace at expand time."
  {:clj-kondo/macro true}
  [bindings & body]
  {:pre [(h/binding-vec? bindings)]}
  `(let ~bindings ~@body))

(mylet [a 12 b 34] (+ a b))
