(ns clj-kondo.impl.var-info
  {:no-doc true}
  (:require [clj-kondo.impl.var-info-gen]))

(declare clojure-core-syms cljs-core-syms)

(def special-forms '#{def if do let quote fn fn* loop recur throw try
                      monitor-enter monitor-exit set! . .. new})

(defn core-sym? [lang sym]
  (case lang
    :clj (contains? clojure-core-syms sym)
    :cljs (contains? cljs-core-syms sym)))

;;;; Scratch

(comment
  )
