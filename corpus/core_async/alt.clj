(ns core-async.alt
  (:require [clojure.core.async :as a]
            [clojure.string :as str]))

(def c (a/chan)) (def t (a/timeout 10000))
(a/alt! [c t] ([v ch] [ch v]) ;; no unresolved symbol warnings
        x1 x2) ;; unresolved symbols

;; no unresolved symbol warnings
;; namespace clojure.string is loaded from cache, so invalid arity
(a/alt!! [c t] ([v ch] (str/join "\n" [ch v] 1))
         x3 x4) ;; unresolved symbols
