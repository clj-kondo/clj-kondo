(ns clj-kondo.impl.analyzer.potemkin
  {:no-doc true}
  (:refer-clojure :exclude [ns-name])
  (:require
   [clj-kondo.impl.analyzer.common :as common]
   [clj-kondo.impl.namespace :as namespace]
   [clj-kondo.impl.utils :as utils :refer [token-node]]))

(defn analyze-import-fn
  "Analyzes potemkin's import-fn, import-macro, and import-def which take
  a single fully-qualified symbol and optionally a new name."
  [ctx expr ctx-with-linters-disabled defined-by defined-by->lint-as]
  (let [ns (:ns ctx)
        ns-name (:name ns)
        qualify-ns (:qualify-ns ns)
        args (next (:children expr))
        sym-node (first args)
        sym-val (:value sym-node)
        rename-node (second args)
        rename-val (:value rename-node)]
    (when (and sym-val (qualified-symbol? sym-val))
      (let [imported-ns (symbol (namespace sym-val))
            imported-ns (qualify-ns imported-ns imported-ns)
            imported-var (symbol (name sym-val))
            local-name (or rename-val imported-var)
            expr-meta (meta sym-node)]
        (common/analyze-usages2
         (ctx-with-linters-disabled ctx [:unresolved-var :unresolved-symbol])
         sym-node)
        (namespace/reg-var! ctx ns-name local-name expr {:imported-ns imported-ns
                                                         :imported-var imported-var
                                                         :name-row (:row expr-meta)
                                                         :name-col (:col expr-meta)
                                                         :name-end-row (:end-row expr-meta)
                                                         :name-end-col (:end-col expr-meta)
                                                         :defined-by defined-by
                                                         :defined-by->lint-as defined-by->lint-as})
        [{:type :import-vars
          :used-namespaces [imported-ns]}]))))

(defn analyze-import-vars [ctx expr ctx-with-linters-disabled defined-by defined-by->lint-as]
  (let [ns (:ns ctx)
        ns-name (:name ns)
        import-groups (next (:children expr))
        qualify-ns (:qualify-ns ns)]
    [{:type :import-vars
      :used-namespaces
      (doall (for [g import-groups
                   :let [gval (:value g)
                         fqs-import? (and gval (qualified-symbol? gval))
                         gchildren (:children g)
                         imported-ns (if fqs-import?
                                       (symbol (namespace gval))
                                       (:value (first gchildren)))
                         imported-ns (qualify-ns imported-ns imported-ns)
                         rest-children (rest gchildren)
                         refer-syntax? (and (not fqs-import?)
                                            (= :refer (:k (first rest-children))))
                         renames (when refer-syntax?
                                   (let [after-refers (drop 2 rest-children)]
                                     (when (= :rename (:k (first after-refers)))
                                       (apply hash-map
                                              (map :value (:children (second after-refers)))))))
                         imported-vars (if fqs-import?
                                         [[g (symbol (name gval)) nil]]
                                         (if refer-syntax?
                                           (map (fn [c]
                                                  (let [v (:value c)]
                                                    [c v (get renames v)]))
                                                (:children (second rest-children)))
                                           (map (fn [c] [c (:value c) nil]) rest-children)))]]
               (do
                 (doseq [[i-expr i-value i-rename] imported-vars]
                   (let [local-name (or i-rename i-value)]
                     (common/analyze-usages2
                      (ctx-with-linters-disabled ctx [:unresolved-var :unresolved-symbol])
                      (if fqs-import?
                        i-expr
                        (with-meta (token-node
                                    (symbol (str (:value (first gchildren)))
                                            (str i-value)))
                          (meta i-expr))))
                     (let [expr-meta (meta i-expr)]
                       (namespace/reg-var! ctx ns-name local-name expr {:imported-ns imported-ns
                                                                        :imported-var i-value
                                                                        :name-row (:row expr-meta)
                                                                        :name-col (:col expr-meta)
                                                                        :name-end-row (:end-row expr-meta)
                                                                        :name-end-col (:end-col expr-meta)
                                                                        :defined-by defined-by
                                                                        :defined-by->lint-as defined-by->lint-as}))))
                 imported-ns)))}]))
