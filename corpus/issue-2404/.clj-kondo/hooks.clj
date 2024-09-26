(ns hooks)

(defn foo [m]
  (update m :node vary-meta assoc :clj-kondo/ignore [:type-mismatch]))
