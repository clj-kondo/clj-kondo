(ns clj-kondo.impl.var-info
  {:no-doc true}
  (:require [clj-kondo.impl.var-info-gen]))

(declare clojure-core-syms cljs-core-syms)

;; in addition to what `special-form?` regards as special:
(def special-forms '#{.. let fn loop def if recur var do quote throw try catch 
                      finally monitor-enter monitor-exit new set!})

(defn core-sym? [lang sym]
  (case lang
    :clj (contains? clojure-core-syms sym)
    :cljs (contains? cljs-core-syms sym)))

;;;; Scratch

(comment
  )
