(ns corpus.cljc.test-cljc
  (:require-macros [corpus.cljc.test-cljc :as c :refer [foo]]))

(foo 1) ;; correct
(foo 1 2) ;; incorrect

(bar 1 2 3) ;; this call should not be recognized, since we didn't refer bar
