(ns corpus.cljc.test-cljs
  (:require [corpus.cljc.test-cljc :refer [foo bar]]))

(foo 1) ;; correct
(foo 1 2) ;; incorrect
(bar 1 2) ;; incorrect

(defn baz [])
