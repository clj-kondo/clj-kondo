(ns aaaa-this-has-to-be-first.because-patches)

(when (System/getenv "CLJ_KONDO_NATIVE")
  (require '[aaaa-this-has-to-be-first.pprint]))
