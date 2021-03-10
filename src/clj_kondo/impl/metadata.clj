(ns clj-kondo.impl.metadata
  {:no-doc true}
  (:require
    [clj-kondo.impl.analyzer.common :as common]
    [clj-kondo.impl.linters.keys :as key-linter]
    [clj-kondo.impl.utils :as utils]))

(defn meta-node->map [ctx node]
  (let [s (utils/sexpr node)]
    (cond (keyword? s) {s true}
          (map? s)
          (do
            (key-linter/lint-map-keys ctx node)
            s)
          :else {:tag s})))

(def type-hint-bindings
  '{void {} objects {}})

(defn lift-meta-content2
  ([ctx node] (lift-meta-content2 ctx node false))
  ([{:keys [:lang] :as ctx} node only-usage?]
   (if-let [meta-list (:meta node)]
     (let [cljs? (identical? :cljs lang)
           maybe-type-hint (and cljs? (utils/symbol-from-token node))
           ignore-type-hint? (and cljs?
                                  maybe-type-hint
                                  (contains? (:bindings ctx) maybe-type-hint))
           ctx (if ignore-type-hint?
                 (assoc-in ctx [:config :linters :unresolved-symbol :level] :off)
                 ctx)
           meta-ctx
           (-> ctx
               (update :callstack conj [nil :metadata])
               (utils/ctx-with-bindings
                (cond->
                    type-hint-bindings
                  cljs?
                  (assoc 'js {}
                         'number {}))))
           ;; use dorun to force analysis, we don't use the end result!
           _ (if only-usage?
               (run! #(dorun (common/analyze-usages2 meta-ctx %))
                     meta-list)
               (run! #(dorun (common/analyze-expression** meta-ctx %))
                     meta-list))
           meta-maps (map #(meta-node->map ctx %) meta-list)
           meta-map (apply merge meta-maps)
           node (-> node
                    (dissoc :meta)
                    (with-meta (merge (meta node) meta-map)))]
       node)
     node)))

;;;; Scratch

(comment
  (meta (lift-meta-content2 {:findings (atom [])} (clj-kondo.impl.utils/parse-string "^{:a 1 :a 2} []")))
  )
