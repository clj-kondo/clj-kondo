(ns hook-generated-ns.app
  (:require [clj-kondo.hooks-api :as api]))

(defn bar [{:keys [node]}]
  {:node (api/list-node
          (list* (api/token-node 'my.library/foo)
                 (rest (:children node))))})
