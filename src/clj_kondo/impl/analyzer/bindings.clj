(ns clj-kondo.impl.analyzer.bindings)

(defn lift-meta-content*
  "Used within extract-bindings. Disables unresolved symbols while
  linting metadata."
  [{:keys [:lang] :as ctx} expr]
  (meta/lift-meta-content2
   (if (= :cljs lang)
     (ctx-with-linter-disabled ctx :unresolved-symbol)
     ctx)
   expr))

(defn extract-bindings
  ([ctx expr] (when expr
                (extract-bindings ctx expr {})))
  ([{:keys [:skip-reg-binding?] :as ctx} expr
    {:keys [:keys-destructuring? :fn-args?] :as opts}]
   (let [expr (lift-meta-content* ctx expr)
         t (tag expr)
         findings (:findings ctx)
         skip-reg-binding? (or skip-reg-binding?
                               (when (and keys-destructuring? fn-args?)
                                 (-> ctx :config :linters :unused-binding
                                     :exclude-destructured-keys-in-fn-args)))]
     (case t
       :token
       (cond
         ;; symbol
         (utils/symbol-token? expr)
         (let [sym (:value expr)]
           (when (not= '& sym)
             (let [ns (namespace sym)
                   valid? (or (not ns)
                              keys-destructuring?)]
               (if valid?
                 (let [s (symbol (name sym))
                       m (meta expr)
                       t (or (types/tag-from-meta (:tag m))
                             (:tag opts))
                       v (assoc m
                                :name s
                                :filename (:filename ctx)
                                :tag t)]
                   (when-not skip-reg-binding?
                     (namespace/reg-binding! ctx
                                             (-> ctx :ns :name)
                                             v))
                   (with-meta {s v} (when t {:tag t})))
                 (findings/reg-finding!
                  findings
                  (node->line (:filename ctx)
                              expr
                              :error
                              :syntax
                              (str "unsupported binding form " sym)))))))
         ;; keyword
         (:k expr)
         (let [k (:k expr)]
           (usages/analyze-keyword ctx expr)
           (if keys-destructuring?
             (let [s (-> k name symbol)
                   m (meta expr)
                   v (assoc m
                            :name s
                            :filename (:filename ctx))]
               (when-not skip-reg-binding?
                 (namespace/reg-binding! ctx
                                         (-> ctx :ns :name)
                                         v))
               {s v})
             ;; TODO: we probably need to check if :as is supported in this
             ;; context, e.g. seq-destructuring?
             (when (not= :as k)
               (findings/reg-finding!
                findings
                (node->line (:filename ctx)
                            expr
                            :error
                            :syntax
                            (str "unsupported binding form " k))))))
         :else
         (findings/reg-finding!
          findings
          (node->line (:filename ctx)
                      expr
                      :error
                      :syntax
                      (str "unsupported binding form " expr))))
       :vector (let [v (map #(extract-bindings ctx % opts) (:children expr))
                     tags (map :tag (map meta v))
                     expr-meta (meta expr)
                     t (:tag expr-meta)
                     t (when t (types/tag-from-meta t true ;; true means it's a
                                                    ;; return type
                                                    ))]
                 (with-meta (into {} v)
                   ;; this is used for checking the return tag of a function body
                   (assoc expr-meta
                          :tag t
                          :tags tags)))
       :namespaced-map (extract-bindings ctx (first (:children expr)) opts)
       :map
       (loop [[k v & rest-kvs] (:children expr)
              res {}]
         (if k
           (let [k (lift-meta-content* ctx k)]
             (cond (:k k)
                   (do
                     (analyze-usages2 ctx k)
                     (case (keyword (name (:k k)))
                       (:keys :syms :strs)
                       (recur rest-kvs
                              (into res (map #(extract-bindings
                                               ctx %
                                               (assoc opts :keys-destructuring? true)))
                                    (:children v)))
                       ;; or doesn't introduce new bindings, it only gives defaults
                       :or
                       (if (empty? rest-kvs)
                         ;; or can refer to a binding introduced by what we extracted
                         (let [ctx (ctx-with-bindings ctx res)]
                           (recur rest-kvs (merge res {:analyzed (analyze-keys-destructuring-defaults
                                                                  ctx res v)})))
                         ;; analyze or after the rest
                         (recur (concat rest-kvs [k v]) res))
                       :as (recur rest-kvs (merge res (extract-bindings ctx v opts)))
                       (recur rest-kvs res)))
                   :else
                   (recur rest-kvs (merge res (extract-bindings ctx k opts)
                                          {:analyzed (analyze-expression** ctx v)}))))
           res))
       (findings/reg-finding!
        findings
        (node->line (:filename ctx)
                    expr
                    :error
                    :syntax
                    (str "unsupported binding form " expr)))))))
