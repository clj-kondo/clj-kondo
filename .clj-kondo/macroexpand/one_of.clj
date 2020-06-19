;; (one-of x [foo bar]), foo bar are literal symbols

(require '[clj-kondo.hooks-api :as api])

(fn [{:keys [:node]}]
  (let [[matchee matches] (rest (:children node))
        new-node (api/list-node
                  [(api/token-node 'case)
                   matchee
                   (api/list-node matches)
                   matchee])]
    {:node new-node}))
