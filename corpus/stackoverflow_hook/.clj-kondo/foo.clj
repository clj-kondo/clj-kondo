(ns foo)

;; Returns (do (my-macro ...)) which re-triggers expansion infinitely
(defmacro my-macro [& body]
  (list 'do (list* 'foo/my-macro body)))
