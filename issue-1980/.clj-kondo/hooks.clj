(ns hooks
  (:require [clj-kondo.hooks-api :as api]))

(defn my-print [{:keys [node]}]
  {:node (with-meta (api/list-node (list* (api/token-node 'clojure.core/println) (rest (:children node))))
           {:clj-kondo/ignore [:discouraged-var]})})
