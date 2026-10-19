(ns macros
  (:require [helpers :as h]
            [wrappers :as w]))

(defmacro direct
  {:clj-kondo/macroexpand-hook true}
  [x]
  `(h/process ~x))

(defmacro via-hook
  "Expands to a call of another macroexpand hook."
  {:clj-kondo/macroexpand-hook true}
  [x]
  `(w/hook-wrap (h/process ~x)))

(defmacro via-hook-do
  {:clj-kondo/macroexpand-hook true}
  [x]
  `(w/hook-wrap (do (h/process ~x))))

(defmacro via-plain
  {:clj-kondo/macroexpand-hook true}
  [x]
  `(w/plain-wrap (h/process ~x)))
