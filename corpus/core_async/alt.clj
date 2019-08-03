(ns core-async.alt
  (:require [clojure.core.async :as a]))

(def c (a/chan)) (def t (a/timeout 10000))
(a/alt! [c t] ([v ch] [ch v]) ;; no unresolved symbol warnings
        x1 x2) ;; unresolved symbols

(a/alt!! [c t] ([v ch] [ch v]) ;; no unresolved symbol warnings
        x3 x4) ;; unresolved symbols

