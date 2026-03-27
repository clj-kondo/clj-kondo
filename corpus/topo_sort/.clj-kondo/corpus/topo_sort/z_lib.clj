(ns corpus.topo-sort.z-lib
  (:require [clj-kondo.hooks-api :as api]))

(defn my-fn-hook [{:keys [node]}]
  (let [analysis (api/ns-analysis 'corpus.topo-sort.z-lib)]
    (when-not (get-in analysis [:clj 'my-fn])
      (let [m (meta node)]
        (api/reg-finding!
         (assoc m
                :message "ns-analysis could not find my-fn - lib not yet analyzed"
                :type :discouraged-var))))))
