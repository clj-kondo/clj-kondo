(ns hooks.demo.macros
  (:require [clj-kondo.hooks-api :as api]))

(defn defvalue [{:keys [node]}]
  (let [[_ sym value] (:children node)]
    {:node (api/list-node [(api/token-node 'def) sym value])}))
