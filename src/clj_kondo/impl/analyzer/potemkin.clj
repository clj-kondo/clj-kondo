(ns clj-kondo.impl.analyzer.potemkin
  {:no-doc true}
  (:refer-clojure :exclude [ns-name])
  (:require
   [clj-kondo.impl.analyzer.common :as common]
   [clj-kondo.impl.namespace :as namespace]
   [clj-kondo.impl.utils :as utils :refer [token-node]]))

(defn analyze-import-vars [ctx expr ctx-with-linters-disabled defined-by defined-by->lint-as]
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
                                  [[g (symbol (name gval))]]
                                  (map (fn [c] [c (:value c)]) (rest gchildren)))]]
        (do
          (doseq [[i-expr i-value] imported-vars]
            (common/analyze-usages2
             (ctx-with-linters-disabled ctx [:unresolved-var :unresolved-symbol])
             (if fqs-import?
               i-expr
               (with-meta (token-node
                           (symbol (str (:value (first gchildren)))
                                   (str i-value)))
                 (meta i-expr))))
            (let [expr-meta (meta i-expr)]
              (namespace/reg-var! ctx ns-name i-value expr {:imported-ns imported-ns
                                                            :imported-var i-value
                                                            :name-row (:row expr-meta)
                                                            :name-col (:col expr-meta)
                                                            :name-end-row (:end-row expr-meta)
                                                            :name-end-col (:end-col expr-meta)
                                                            :defined-by defined-by
                                                            :defined-by->lint-as defined-by->lint-as})))
          imported-ns))}]))
