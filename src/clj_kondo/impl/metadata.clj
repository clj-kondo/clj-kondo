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
  '{void {} objects {} number {}})

(defn lift-meta-content2
  ([ctx node] (lift-meta-content2 ctx node false))
  ([{:keys [:lang] :as ctx} node only-usage?]
   (if-let [meta-list (:meta node)]
     (let [meta-ctx
           (-> ctx
               (update :callstack conj [nil :metadata])
               (utils/ctx-with-bindings
                (cond->
                    type-hint-bindings
                  (identical? :cljs lang)
                  (assoc 'js {}))))
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
