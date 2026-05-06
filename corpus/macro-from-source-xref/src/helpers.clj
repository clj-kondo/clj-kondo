(ns helpers)

(defn ^{:clj-kondo/macro true} binding-vec?
  "Helper consumed at expand time by macros in another namespace."
  [v]
  (and (vector? v) (even? (count v))))
