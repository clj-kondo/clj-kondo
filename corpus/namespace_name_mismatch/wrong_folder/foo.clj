(ns namespace-name-mismatch.something.foo)

;; This should trigger the linter :namespace-name-mismatch
;; because the folder name should have been `something`
;; instead of `wrong_folder`.
