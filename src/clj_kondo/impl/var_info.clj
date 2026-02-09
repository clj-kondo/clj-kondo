(ns clj-kondo.impl.var-info
  {:no-doc true}
  (:require
   [clj-kondo.impl.var-info-gen :refer [cljs-core-syms clojure-core-syms]]))

;; in addition to what `special-form?` regards as special:
(def special-forms '#{.. let fn loop})

(defn core-sym? [lang sym]
  (case lang
    :clj (contains? clojure-core-syms sym)
    :cljs (contains? cljs-core-syms sym)))

;;;; Scratch

(comment
  )
