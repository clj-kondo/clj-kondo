(ns namespace-name-mismatch.folder-with-dashes.foo)

;; This should trigger the linter :namespace-name-mismatch
;; because the folder name should have been `folder_with_dashes`
;; with underscores instead of dashes.
