(ns namespace-name-mismatch.foo)

;; This should trigger the linter :namespace-name-mismatch
;; because the namespace should have been 'wrong-file'.

(ns namespace-name-mismatch.foo2) ;; this should not be reported
