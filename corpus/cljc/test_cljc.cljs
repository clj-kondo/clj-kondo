(ns corpus.cljc.test-cljc
  (:require [corpus.cljc.test-cljc :refer [foo]]))

(foo 1) ;; correct
(foo 1 2) ;; incorrect
