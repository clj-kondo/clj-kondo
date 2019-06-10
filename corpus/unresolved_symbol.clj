(ns ^{:clj-kondo/config '{:linters {:unresolved-symbol {:exclude [unresolved-fn1]}}}}
    unresolved-symbol)

(comment
  (unresolved-fn1)
  (unresolved-fn2))
