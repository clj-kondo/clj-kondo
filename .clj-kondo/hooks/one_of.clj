;; (one-of x [foo bar]), foo bar are literal symbols

(ns hooks.one-of
  (:require [clj-kondo.hooks-api :as api]))

(defn one-of [{:keys [node]}]
  (let [[matchee matches] (rest (:children node))
        new-node (api/list-node
                  [(api/token-node 'case)
                   matchee
                   (with-meta (api/list-node (:children matches))
                     (meta matches))
                   matchee])]
    {:node new-node}))
