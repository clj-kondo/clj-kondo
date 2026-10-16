(ns demo.api
  (:require [demo.macros :as macros :refer [defvalue]]))

(defmacro defvalue-aliased
  {:clj-kondo/macroexpand-hook true}
  [sym]
  `(macros/defvalue ~sym 1))

(defmacro defvalue-referred
  {:clj-kondo/macroexpand-hook true}
  [sym]
  `(defvalue ~sym 1))
