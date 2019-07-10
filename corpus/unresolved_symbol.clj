(ns ^{:clj-kondo/config '{:linters {:unresolved-symbol {:exclude [unresolved-fn1]}}}}
    unresolved-symbol)

(comment
  (unresolved-fn1))

(ns ^{:clj-kondo/config '{:linters {:unresolved-symbol {:exclude [unresolved-fn2]}}}}
    unresolved-symbol2)

(comment
  (unresolved-fn1)
  (unresolved-fn2))

foo/bar ;; <- unresolved
clojure.string/join ;; <- unresolved

(require '[clojure.set])
clojure.set/join ;; <- should not be unresolved anymore
