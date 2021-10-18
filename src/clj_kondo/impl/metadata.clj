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
  ([{:keys [:analyze-meta? :lang] :as ctx} node only-usage?]
   (if-let [meta-list (:meta node)]
     (let [meta-list (if (identical? :cljc (:base-lang ctx))
                       (map #(utils/select-lang % lang) meta-list)
                       meta-list)
           cljs? (identical? :cljs lang)
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
           meta-map (if analyze-meta?
                      (assoc meta-map :user-meta [meta-map])
                      meta-map)
           node (dissoc node :meta)
           node (let [new-meta (->
                                ;; clear user-coded metadata that can conflict with clj-kondo
                                ;; clj-kondo only sometimes sets these but later always checks them
                                (dissoc meta-map :name-row :name-col :name-end-row :name-end-col))
                      new-meta (merge new-meta (meta node))]
                  (with-meta node new-meta))]
       node)
     node)))

;;;; Scratch

(comment
  (meta (lift-meta-content2 {:findings (atom [])} (clj-kondo.impl.utils/parse-string "^{:a 1 :a 2} []")))
  )
