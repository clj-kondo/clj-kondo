(ns hooks
  (:require [clj-kondo.hooks-api :as api]))

(defn dispatch [{:keys [node]}]
  ;; rewrite (dispatch <args>) => (vector <args>), reusing the original arg nodes
  (let [[_ & args] (:children node)]
    {:node (api/list-node (list* (api/token-node 'vector) args))}))
