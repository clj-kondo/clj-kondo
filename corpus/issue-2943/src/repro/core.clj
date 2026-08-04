(ns repro.core)

;; get gives client an inferred param type
(defn dispatch [_event-name client]
  (get client :k))

(defn use-it []
  (dispatch :event {:a 1})
  ;; the rewritten call is still arity checked
  (dispatch :event))
