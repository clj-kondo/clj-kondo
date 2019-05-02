(ns clj-kondo.impl.var-info
  {:no-doc true}
  (:require [clj-kondo.impl.var-info-gen]))

(def special-forms '#{def if do let quote fn loop recur throw try monitor-enter monitor-exit set!})

;;;; Scratch

(comment
  (first predicates)
  )
