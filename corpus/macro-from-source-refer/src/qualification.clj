(ns qualification
  (:require [clojure.string :refer [join]]
            [clojure.test :refer [deftest] :rename {deftest defcheck}]))

(defmacro defcheck-wrapper
  {:clj-kondo/macroexpand-hook true}
  [sym]
  `(defcheck ~sym))

(defmacro defjoined
  {:clj-kondo/macroexpand-hook true}
  [sym join]
  `(def ~sym (join "," ~join)))

(defmacro quoted-symbol
  {:clj-kondo/macroexpand-hook true}
  []
  '(quote join))
