(ns redundant-do)

(do)
(do 1 (do 2))
(defn foo [] (do 1 2 3))
(fn [] (do 1 2))
(let [x 1] 1 2 (do 1 2 3))
(when (do 1 2 3)) ;; no mention of redundant do but mention of missing body in when
(when :foo (do 1 2 3))
(when-not :foo (do 1 2 3))
(future (do 1 2))
(when-let [x 1] (do x 2))
