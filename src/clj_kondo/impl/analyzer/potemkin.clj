(ns clj-kondo.impl.analyzer.potemkin
  {:no-doc true}
  (:refer-clojure :exclude [ns-name])
  (:require [clj-kondo.impl.namespace :as namespace]))

(defn analyze-import-vars [ctx expr]
  (let [ns-name (-> ctx :ns :name)
        import-groups (next (:children expr))]
    [{:type :import-vars
      :used-namespaces
      (for [g import-groups
            :let [[imported-ns & imported-vars] (:children g)
                  imported-ns-sym (:value imported-ns)]]
        (do (doseq [iv imported-vars]
              (let [iv-sym (:value iv)]
                (namespace/reg-var! ctx ns-name iv-sym expr {:imported-ns imported-ns-sym
                                                             :imported-var iv-sym})))
            imported-ns-sym))}]))
