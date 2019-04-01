(ns obsolete-do)

(do)
(do 1 (do 2))
(defn foo [] (do 1 2 3))
(fn [] (do 1 2))
(let [] 1 2 (do 1 2 3))
