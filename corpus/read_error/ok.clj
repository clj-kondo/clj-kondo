(ns read-error.ok)

(defn foo [] 1)

;; this should still yield an error
(foo 1)
