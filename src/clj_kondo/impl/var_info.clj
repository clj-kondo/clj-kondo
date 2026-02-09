(ns clj-kondo.impl.var-info
  {:no-doc true}
  (:require [clj-kondo.impl.var-info-gen :as var-info-gen]))

(def default-fq-imports var-info-gen/default-fq-imports)
(def default-import->qname var-info-gen/default-import->qname)
(def unused-values var-info-gen/unused-values)
;; in addition to what `special-form?` regards as special:
(def special-forms '#{.. let fn loop})

(defn core-sym? [lang sym]
  (case lang
    :clj (contains? var-info-gen/clojure-core-syms sym)
    :cljs (contains? var-info-gen/cljs-core-syms sym)))

;;;; Scratch

(comment
  )
