(ns clj-kondo.impl.var-info
  {:no-doc true}
  (:require [clj-kondo.impl.var-info-gen]))

^{:clj-kondo/ignore [:redundant-declare]}
(declare clojure-core-syms cljs-core-syms)

;; in addition to what `special-form?` regards as special:
(def special-forms '#{.. let fn loop})

(defn core-sym? [lang sym]
  (case lang
    :clj (contains? clojure-core-syms sym)
    :cljs (contains? cljs-core-syms sym)))

;;;; Scratch

(comment
  )
