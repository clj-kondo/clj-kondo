#_{:clj-kondo/ignore [:namespace-name-mismatch]}
(ns ignore-that-the-namespace-does-not-match-the-filename)

;; This should NOT trigger the linter :namespace-name-mismatch
;; because the linter is ignored.
