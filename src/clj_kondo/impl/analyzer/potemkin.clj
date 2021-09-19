(ns clj-kondo.impl.analyzer.potemkin
  {:no-doc true}
  (:refer-clojure :exclude [ns-name])
  (:require [clj-kondo.impl.namespace :as namespace]))

(defn analyze-import-vars [ctx expr]
  (let [ns (:ns ctx)
        ns-name (:name ns)
        import-groups (next (:children expr))
        qualify-ns (:qualify-ns ns)]
    [{:type :import-vars
      :used-namespaces
      (for [g import-groups
            :let [gval (:value g)
                  fqs-import? (and gval (qualified-symbol? gval))
                  gchildren (:children g)
                  imported-ns (if fqs-import?
                                (symbol (namespace gval))
                                (:value (first gchildren)))
                  imported-ns (qualify-ns imported-ns imported-ns)
                  imported-vars (if fqs-import?
                                  [(symbol (name gval))]
                                  (map :value (rest gchildren)))]]
        (do (doseq [iv imported-vars]
              (namespace/reg-var! ctx ns-name iv expr {:imported-ns imported-ns
                                                       :imported-var iv}))
            imported-ns))}]))
