;; deliberately the same ns as corpus/hook-ns-clash/b/.clj-kondo/hooks.clj
(ns hooks
  (:require [clj-kondo.hooks-api :as api]))

;; (foo x) => (inc x x)
(defn a-hook [{:keys [node]}]
  (let [[_ & args] (:children node)]
    {:node (api/list-node (list* (api/token-node 'inc) (concat args args)))}))
