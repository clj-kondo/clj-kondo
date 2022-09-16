(require '[clj-kondo.hooks-api :as api])

(fn [{:keys [node]}]
  (let [children (rest (:children node))
        node (list* (api/token-node 'clojure.core/->) children)]
    {:node (api/list-node node)}))
