;; deliberately the same ns as corpus/hook-ns-clash/a/.clj-kondo/hooks.clj
(ns hooks
  (:require [clj-kondo.hooks-api :as api]))

;; (bar x) => (dec x x x)
(defn b-hook [{:keys [node]}]
  (let [[_ & args] (:children node)]
    {:node (api/list-node (list* (api/token-node 'dec) (concat args args args)))}))
