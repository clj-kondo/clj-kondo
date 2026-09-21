(ns hook-generated-ns.lib
  (:require [clj-kondo.hooks-api :as api]))

(defn foo [{:keys [node]}]
  (let [[_ sym & body] (:children node)]
    {:node (api/list-node
            (list* (api/token-node 'let)
                   (api/vector-node [sym (api/token-node 1)])
                   body))}))
