(ns helpers)

(defn ^{:clj-kondo/macroexpand-hook true} binding-vec?
  "Helper consumed at expand time by macros in another namespace."
  [v]
  (and (vector? v) (even? (count v))))
