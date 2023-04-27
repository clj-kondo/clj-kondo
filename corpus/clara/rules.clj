(ns clara.rules)

(defmacro defrule
  [name & body]
  `(def ^:query ~name '~body))

(defmacro defquery
  [name & body]
  `(def ^:rule ~name '~body))

(declare insert!)
