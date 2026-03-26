(ns macros2
  {:clj-kondo/ignore [:unused-binding]})

(let [x 1]) ;; no warning about unused binding
(inc :foo) ;; this warning is still displayed

(defmacro with-foo
  {:clj-kondo/lint-as 'clojure.core/let}
  [bnds & body]
  `(let [~@bnds]
     ~@body))

(with-foo [a 1]
  a) ;; no warning, macro configured as clojure.core/let

(defmacro matcher
  {:clj-kondo/ignore [:unresolved-symbol :type-mismatch]}
  [m match-expr & body]
  ;; dummy
  [m match-expr body])

(matcher {:a 1} {?a 1} (inc :foo)) ;; no warning in usage of macro
