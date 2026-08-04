;; a unique ns: hook namespaces share one SCI context, so a plain `hooks` would
;; clash with the other corpus hooks of that name
(ns hooks.issue-2943
  (:require [clj-kondo.hooks-api :as api]))

(defn dispatch [{:keys [node]}]
  ;; rewrite (dispatch <args>) => (vector <args>), reusing the original arg nodes
  (let [[_ & args] (:children node)]
    {:node (api/list-node (list* (api/token-node 'vector) args))}))
