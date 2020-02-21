(ns ^{:clj-kondo/config '{:linters {:unresolved-symbol {:exclude [unresolved-fn1]}}}}
    unresolved-symbol)

(comment
  (unresolved-fn1))

(ns ^{:clj-kondo/config '{:linters {:unresolved-symbol {:exclude [unresolved-fn2]}}}}
    unresolved-symbol2)

(comment
  (unresolved-fn1)
  (unresolved-fn2))

(require '[clojure.set :refer [union]])
(clojure.set/join) ;; <- should be resolved
union ;; <- also resolved

(foo 1) ;; using a function before its definition is unresolved
(defn foo [])

(declare bar)
(bar 1) ;; but declaring a var and then calling it is not unresolved. moreover,
        ;; this is the wrong arity, so an error
(defn bar [])
