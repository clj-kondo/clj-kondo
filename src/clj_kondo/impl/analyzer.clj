(ns clj-kondo.impl.analyzer
  {:no-doc true}
  (:refer-clojure :exclude [ns-name])
  (:require
   [babashka.fs :as fs]
   [clj-kondo.impl.analysis :as analysis]
   [clj-kondo.impl.analyzer.babashka :as babashka]
   [clj-kondo.impl.analyzer.clojure-data-xml :as xml]
   [clj-kondo.impl.analyzer.common :refer [common]]
   [clj-kondo.impl.analyzer.compojure :as compojure]
   [clj-kondo.impl.analyzer.core-async :as core-async]
   [clj-kondo.impl.analyzer.datalog :as datalog]
   [clj-kondo.impl.analyzer.jdbc :as jdbc]
   [clj-kondo.impl.analyzer.match :as match]
   [clj-kondo.impl.analyzer.namespace :as namespace-analyzer
    :refer [analyze-ns-decl]]
   [clj-kondo.impl.analyzer.potemkin :as potemkin]
   [clj-kondo.impl.analyzer.re-frame :as re-frame]
   [clj-kondo.impl.analyzer.spec :as spec]
   [clj-kondo.impl.analyzer.test :as test]
   [clj-kondo.impl.analyzer.usages :as usages :refer [analyze-usages2]]
   [clj-kondo.impl.cache :as cache]
   [clj-kondo.impl.config :as config]
   [clj-kondo.impl.docstring :as docstring]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.hooks :as hooks]
   [clj-kondo.impl.linters :as linters]
   [clj-kondo.impl.linters.config :as lint-config]
   [clj-kondo.impl.linters.deps-edn :as deps-edn]
   [clj-kondo.impl.linters.keys :as key-linter]
   [clj-kondo.impl.macroexpand :as macroexpand]
   [clj-kondo.impl.metadata :as meta]
   [clj-kondo.impl.namespace :as namespace :refer [resolve-name]]
   [clj-kondo.impl.parser :as p]
   [clj-kondo.impl.rewrite-clj.node.seq :as seq]
   [clj-kondo.impl.rewrite-clj.node.token :as token]
   [clj-kondo.impl.rewrite-clj.reader :refer [*reader-exceptions*]]
   [clj-kondo.impl.schema :as schema]
   [clj-kondo.impl.types :as types]
   [clj-kondo.impl.utils :as utils :refer
    [assoc-some ctx-with-bindings deep-merge linter-disabled? node->line
     one-of parse-string select-lang sexpr string-from-token symbol-call tag
     tag]]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [sci.core :as sci]))

(set! *warn-on-reflection* true)

(declare analyze-expression**)

(defn analyze-children
  ([ctx children]
   (analyze-children ctx children true))
  ([{:keys [:callstack :config :top-level?] :as ctx} children add-new-arg-types?]
   (let [top-level? (and top-level?
                         (let [fst (first callstack)]
                           (one-of fst [[clojure.core comment]
                                        [cljs.core comment]
                                        [clojure.core do]
                                        [cljs.core do]
                                        [clojure.core let]
                                        [cljs.core let]])))]
     (when-not (and (:in-comment ctx)
                    (:skip-comments config))
       (let [len (count children)
             ctx (assoc ctx
                        :top-level? top-level?
                        :arg-types (if add-new-arg-types?
                                     (let [[k v] (first callstack)]
                                       (if (and (symbol? k)
                                                (symbol? v))
                                         (atom [])
                                         nil))
                                     (:arg-types ctx))
                        :len len)]
         (into []
               (comp (map-indexed (fn [i e]
                                    (analyze-expression** (assoc ctx :idx i) e)))
                     cat)
               children))))))

(defn analyze-keys-destructuring-defaults [ctx prev-ctx m defaults opts]
  #_(prn :anathefuck)
  (let [mark-used? (or (:skip-reg-binding? ctx)
                       (:mark-bindings-used? ctx)
                       (when (:fn-args? opts)
                         (-> ctx :config :linters :unused-binding
                             :exclude-destructured-keys-in-fn-args)))]
    (when-not mark-used?
      (doseq [[k _v] (partition 2 (:children defaults))
              :let [sym (:value k)
                    mta (meta k)]
              :when sym]
        (if-let [binding (get m sym)]
          (namespace/reg-destructuring-default! ctx mta binding)
          (findings/reg-finding!
           ctx
           {:message (str sym " is not bound in this destructuring form") :level :warning
            :row (:row mta)
            :col (:col mta)
            :end-row (:end-row mta)
            :end-col (:end-col mta)
            :filename (:filename ctx)
            :type :unbound-destructuring-default})))))
  (let [undefined-locals (set (keys m))]
    (doseq [[k v] (partition 2 (:children defaults))]
      (let [binding (:value k)
            simple? (and (identical? :token (utils/tag k))
                         (simple-symbol? binding))]
        (when-not simple?
          (let [m (meta k)]
            (findings/reg-finding!
             ctx
             {:message "Keys in :or should be simple symbols."
              :row (:row m)
              :col (:col m)
              :end-row (:end-row m)
              :end-col (:end-col m)
              :filename (:filename ctx)
              :type :syntax})))
        (if (= k v)
          ;; see #915
          (analyze-expression** prev-ctx v)
          (do
            (when (and (:analyze-locals? ctx) (not (:clj-kondo/mark-used k)))
              (let [expr-meta (meta k)
                    expr-meta (assoc-some expr-meta
                                          :name-row (:row expr-meta)
                                          :name-col (:col expr-meta)
                                          :name-end-row (:end-row expr-meta)
                                          :name-end-col (:end-col expr-meta)
                                          :name binding
                                          :filename (:filename ctx)
                                          :str (:string-value k))]
                (analysis/reg-local-usage! ctx (:filename ctx) (get (:bindings ctx) binding) expr-meta)))
            (analyze-expression** (assoc ctx :undefined-locals undefined-locals) v)))))))

(defn lift-meta-content*
  "Used within extract-bindings. Disables unresolved symbols while
  linting metadata."
  [{:keys [:lang] :as ctx} expr]
  (meta/lift-meta-content2
   (if (= :cljs lang)
     (utils/ctx-with-linter-disabled ctx :unresolved-symbol)
     ctx)
   expr))

(defn scope-end
  "Used within extract-bindings. Extracts the end postion of the
   scoped-expr for the binding."
  [scoped-expr]
  (some-> scoped-expr
          meta
          (select-keys [:end-row :end-col])
          (set/rename-keys {:end-row :scope-end-row :end-col :scope-end-col})))

(defn extract-bindings
  ([ctx expr] (extract-bindings ctx expr expr {}))
  ([ctx expr scoped-expr opts]
   (when expr
     (utils/handle-ignore ctx expr)
     (let [fn-args? (:fn-args? opts)
           keys-destructuring? (:keys-destructuring? opts)
           expr (lift-meta-content* ctx expr)
           t (tag expr)
           mark-used?
           (or (:mark-bindings-used? ctx)
               (let [unused-binding-cfg (-> ctx :config :linters :unused-binding)]
                 (or (when (and keys-destructuring? fn-args?)
                       (-> unused-binding-cfg
                           :exclude-destructured-keys-in-fn-args))
                     (and (:defmulti? ctx)
                          (-> unused-binding-cfg
                              :exclude-defmulti-args))
                     (identical? :off (:level unused-binding-cfg)))))
           ctx (cond-> ctx
                 mark-used? (assoc :mark-bindings-used? mark-used?))
           only-arity-analysis (:skip-reg-binding? ctx)]
       (case t
         :token
         (cond
           ;; symbol
           (utils/symbol-token? expr)
           (let [sym (:value expr)]
             (when (= :keys (:destructuring-type opts))
               (usages/analyze-keyword ctx expr opts))
             (when-not only-arity-analysis
               (when-let [fn-dupes (:fn-dupes ctx)]
                 (when (and (contains? @fn-dupes sym)
                            (not= '_ sym)
                            (not= '& sym))
                   (findings/reg-finding! ctx (assoc (meta expr) :type :shadowed-fn-param
                                                     :filename (:filename ctx)
                                                     :message (str "Shadowed fn param: " sym))))
                 (swap! fn-dupes conj sym)))
             (when (not= '& sym)
               (let [ns (namespace sym)
                     valid? (or (not ns)
                                keys-destructuring?)]
                 (if valid?
                   (let [s (symbol (name sym))
                         m (meta expr)
                         t (or (types/tag-from-meta (:tag m))
                               (:tag opts))
                         v (cond-> (assoc m
                                          :name s
                                          :filename (:filename ctx)
                                          :tag t
                                          :auto-resolved (:namespaced? expr))
                             (:analyze-locals? ctx)
                             (-> (assoc :id (swap! (:id-gen ctx) inc)
                                        :str (str expr))
                                 (merge (scope-end scoped-expr))))]
                     (namespace/reg-binding! ctx
                                             (-> ctx :ns :name)
                                             v)
                     (namespace/check-shadowed-binding! ctx s expr)
                     (with-meta {s v} (when t {:tag t})))
                   (findings/reg-finding!
                    ctx
                    (node->line (:filename ctx)
                                expr
                                :syntax
                                (str "unsupported binding form " sym)))))))
           ;; keyword
           (:k expr)
           (let [k (:k expr)]
             (usages/analyze-keyword ctx expr opts)
             (if keys-destructuring?
               (let [s (-> k name symbol)
                     m (meta expr)
                     v (cond-> (assoc m
                                      :name s
                                      :keyword k
                                      :filename (:filename ctx)
                                      :auto-resolved (:namespaced? expr))
                         (:analyze-locals? ctx)
                         (-> (assoc :id (swap! (:id-gen ctx) inc)
                                    :str (str expr))
                             (merge (scope-end scoped-expr))))]
                 (namespace/reg-binding! ctx
                                         (-> ctx :ns :name)
                                         v)
                 {s v})
               ;; TODO: we probably need to check if :as is supported in this
               ;; context, e.g. seq-destructuring?
               (when (not= :as k)
                 (findings/reg-finding!
                  ctx
                  (node->line (:filename ctx)
                              expr
                              :syntax
                              (str "unsupported binding form " k))))))
           :else
           (findings/reg-finding!
            ctx
            (node->line (:filename ctx)
                        expr
                        :syntax
                        (str "unsupported binding form " expr))))
         :vector (let [children (:children expr)
                       all-tokens? (every? #(identical? :token %) (map :tag children))
                       exclude-as? (-> ctx :config :linters :unused-binding
                                       :exclude-destructured-as)
                       as-sym (when exclude-as?
                                (let [[as as-sym] (drop (- (count children) 2) children)]
                                  (when (identical? :as (:k as))
                                    as-sym)))
                       v (let [ctx (update ctx :callstack conj [nil :vector])]
                           (if all-tokens?
                             (map #(extract-bindings ctx % scoped-expr opts) children)
                             (-> (reduce (fn [[ctx acc] expr]
                                           (let [ctx (if (and as-sym (= expr as-sym))
                                                       (assoc ctx :mark-bindings-used? true)
                                                       ctx)
                                                 bnds (extract-bindings ctx expr scoped-expr opts)]
                                             [(ctx-with-bindings ctx bnds) (conj! acc bnds)]))
                                         [ctx
                                          (transient [])]
                                         children)
                                 second
                                 persistent!)))
                       tags (map :tag (map meta v))
                       expr-meta (meta expr)
                       t (:tag expr-meta)
                       t (when t (types/tag-from-meta t))]
                   (with-meta (into {} v)
                     ;; this is used for checking the return tag of a function body
                     (assoc expr-meta
                            :tag t
                            :tags tags)))
         :namespaced-map (extract-bindings ctx (first (:children expr)) scoped-expr opts)
         :map
         ;; first check even amount of keys + vals
         (do (key-linter/lint-map-keys ctx expr)
             (loop [[k v & rest-kvs] (:children expr)
                    res {}]
               (if k
                 (let [k (lift-meta-content* ctx k)]
                   (cond (:k k)
                         (let [key-name (keyword (name (:k k)))
                               ns-modifier? (one-of key-name [:keys :syms :strs
                                                              ;; TODO: restrict this to language :cljd
                                                              :flds])]
                           (if ns-modifier?
                             (do (analyze-usages2 ctx k (assoc opts :keys-destructuring-ns-modifier? true))
                                 (recur rest-kvs
                                        (into res (map #(extract-bindings
                                                         ctx
                                                         %
                                                         scoped-expr
                                                         (assoc opts
                                                                :keys-destructuring? true
                                                                :destructuring-type (some-> k :k name keyword)
                                                                :destructuring-expr k)))
                                              (:children v))))
                             (do (analyze-usages2 ctx k)
                                 (case key-name
                                   :or
                                   ;; or doesn't introduce new bindings, it only gives defaults
                                   (if (empty? rest-kvs)
                                     ;; or can refer to a binding introduced by what we extracted
                                     (let [prev-ctx ctx
                                           ctx (ctx-with-bindings ctx res)]
                                       (analyze-keys-destructuring-defaults ctx prev-ctx res v opts)
                                       (recur rest-kvs res))
                                     ;; analyze or after the rest
                                     (let [;; prevent infinite loop with multiple :or
                                           rest-kvs (remove #(= :or (:k %)) rest-kvs)]
                                       (recur (concat rest-kvs [k v]) res)))
                                   :as (if (-> ctx :config :linters :unused-binding
                                               :exclude-destructured-as)
                                         (recur rest-kvs (merge res (extract-bindings (assoc ctx :mark-bindings-used? true) v scoped-expr opts)))
                                         (recur rest-kvs (merge res (extract-bindings ctx v scoped-expr opts))))
                                   (recur rest-kvs res)))))
                         :else
                         (recur rest-kvs (merge res
                                                (extract-bindings ctx k scoped-expr opts)
                                                {:analyzed (analyze-expression** ctx v)}))))
                 res)))
         (findings/reg-finding!
          ctx
          (node->line (:filename ctx)
                      expr
                      :syntax
                      (str "unsupported binding form " expr))))))))

(defn analyze-in-ns [ctx {:keys [:children] :as expr}]
  (let [{:keys [:row :col]} expr
        lang (:lang ctx)
        ns-name (-> children second :children first :value)
        ns (when ns-name
             (-> (namespace-analyzer/new-namespace
                  (:filename ctx)
                  (:base-lang ctx)
                  (:lang ctx)
                  ns-name
                  :in-ns
                  row col)
                 ;; fix fully qualified core references
                 (assoc :qualify-ns (cond-> {}
                                      (identical? :clj lang)
                                      (assoc 'clojure.core 'clojure.core)
                                      (identical? :cljs lang)
                                      (assoc 'cljs.core 'cljs.core
                                             'clojure.core 'cljs.core)))))]
    (namespace/reg-namespace! ctx ns)
    (analyze-children ctx (next children))
    ns))

;;;; function arity

(defn lint-fn-name! [ctx name-node]
  (when-not (simple-symbol? (:value name-node))
    (findings/reg-finding! ctx (assoc (meta name-node)
                                      :message (str "Function name must be simple symbol but got: " (str name-node))
                                      :type :syntax
                                      :filename (:filename ctx)))))

(defn analyze-arity [ctx arg-vec]
  (loop [[arg & rest-args] (:children arg-vec)
         arity 0]
    (if arg
      (if (= '& (:value arg))
        (do
          (when-not (= 1 (count rest-args))
            (findings/reg-finding!
             ctx
             (assoc (meta (second rest-args))
                    :filename (:filename ctx)
                    :type :syntax
                    :message (str "Only one varargs binding allowed but got: "
                                  (str/join ", " rest-args)))))
          {:min-arity arity
           :varargs? true})
        (recur rest-args
               (inc arity)))
      {:fixed-arity arity})))

(defn analyze-fn-arity [ctx body]
  (if-let [a (:analyzed-arity body)]
    a
    (do (utils/handle-ignore ctx body)
        (if-let [children (seq (:children body))]
          (let [arg-vec (first children)
                arg-vec-t (tag arg-vec)]
            (if (not= :vector arg-vec-t)
              (findings/reg-finding! ctx
                                     (node->line (:filename ctx)
                                                 body
                                                 :syntax
                                                 "Function arguments should be wrapped in vector."))
              (let [fn-dupes (atom #{}) ;; used to detect duplicate fn arg names
                    arg-bindings (extract-bindings (assoc ctx :fn-dupes fn-dupes) arg-vec body {:fn-args? true})
                    {return-tag :tag
                     arg-tags :tags} (meta arg-bindings)
                    arity (analyze-arity ctx arg-vec)
                    ret (cond-> {:arg-bindings (dissoc arg-bindings :analyzed)
                                 :arity arity
                                 :analyzed-arg-vec (:analyzed arg-bindings)
                                 :arg-vec arg-vec
                                 :args arg-tags
                                 :ret return-tag}
                          (:analyze-arglists? ctx) (assoc :arglist-str (str arg-vec)))]
                ret)))
          (findings/reg-finding! ctx
                                 (node->line (:filename ctx)
                                             body
                                             :syntax
                                             "Invalid function body."))))))

(defn- meta-arglists-node->strs
  "Return arglist-strs for `node` representing arglists metadata value.
  Returns nil if node does not match expected quoted list of vectors shape, ex: '([x][x y] ...)."
  [node]
  (when (and node
             (= :quote (tag node))
             (= :list (some-> node :children first tag)))
    (reduce (fn [acc arg-vec]
              (if (= :vector (tag arg-vec))
                (conj acc (str arg-vec))
                (reduced nil)))
            []
            (-> node :children first :children))))

(defn- meta-node->arglist-strs
  "Returns arglists-strs for `:arglists` in `node` representing metadata map.
  Returns nil if no `:arglists` found, or found `:arglists` looks invalid."
  [node]
  (some-> node
          (utils/map-node-get-value-node :arglists)
          meta-arglists-node->strs))

(defn analyze-pre-post-map [ctx expr]
  (let [ctx (update ctx :callstack conj [nil :pre-post])
        children (:children expr)]
    (key-linter/lint-map-keys ctx expr)
    (mapcat (fn [[key-expr value-expr]]
              (let [analyzed-key (analyze-expression** ctx key-expr)
                    analyzed-value (if (= :post (:k key-expr))
                                     (analyze-expression**
                                      (ctx-with-bindings ctx '{% {}}) value-expr)
                                     (analyze-expression** ctx value-expr))]
                (concat analyzed-key analyzed-value)))
            (partition 2 children))))

(defn analyze-fn-body [ctx body]
  #_(prn :ana-fn-body)
  #_(prn (:analyzed-arity body))
  (let [docstring (:docstring ctx)
        macro? (:macro? ctx)
        {:keys [:arg-bindings
                :arity :analyzed-arg-vec :arglist-str :arg-vec]
         return-tag :ret
         arg-tags :args} (analyze-fn-arity ctx body)
        ctx (ctx-with-bindings ctx arg-bindings)
        ctx (assoc ctx
                   :recur-arity arity
                   :top-level? false)
        children (next (:children body))
        first-child (first children)
        one-child? (= 1 (count children))
        pre-post-map (when-not one-child?
                       (when (and first-child
                                  (identical? :map (tag first-child))
                                  (some #(let [k (:k %)]
                                           (or (= :pre k)
                                               (= :post k)))
                                        (take-nth 2 (:children first-child))))
                         first-child))
        analyze-pre-post (when pre-post-map
                           (analyze-pre-post-map ctx first-child))
        children (if pre-post-map (next children) children)
        _
        (let [t (when first-child (tag first-child))]
          ;;(prn (first (:callstack ctx)))
          (cond (and (not docstring)
                     (not one-child?)
                     (one-of t [:token :multi-line])
                     (string-from-token first-child)
                     (one-of (first (:callstack ctx))
                             [[clojure.core defn]
                              [cljs.core defn]
                              [clojure.core defn-]
                              [cljs.core defn-]]))
                (findings/reg-finding! ctx
                                       (node->line (:filename ctx)
                                                   first-child
                                                   :misplaced-docstring
                                                   "Misplaced docstring."))))
        ctx (-> ctx
                (assoc :fn-args (:children arg-vec))
                (assoc :body-children-count (count children))
                (assoc :pre-post-map (some? pre-post-map)))
        children (if (:analyze-var-defs-shallowly? ctx)
                   []
                   children)
        [parsed return-tag]
        (if (or macro? return-tag)
          [(analyze-children ctx children) return-tag]
          (let [last-expr (last children)
                ret-expr-id (gensym)
                last-expr (when last-expr (assoc last-expr :id ret-expr-id))
                body-exprs (concat (butlast children) [last-expr])
                parsed (doall (analyze-children ctx body-exprs))
                ret-tag (or
                         return-tag
                         (let [maybe-call (get @(:calls-by-id ctx) ret-expr-id)
                               tag (cond maybe-call (:ret maybe-call)
                                         last-expr (types/expr->tag ctx last-expr))]
                           tag))]
            [parsed ret-tag]))]
    (assoc arity
           :parsed
           (concat analyzed-arg-vec analyze-pre-post parsed)
           :ret return-tag
           :arglist-str arglist-str
           :arg-vec arg-vec
           :args arg-tags)))

(defn fn-bodies [ctx children body]
  (loop [[expr & rest-exprs :as exprs] children]
    (when expr
      (let [expr (meta/lift-meta-content2 ctx expr)
            t (tag expr)]
        (case t
          :vector [(with-meta (utils/list-node exprs)
                     (dissoc (meta body)
                             :clj-kondo/ignore))]
          :list exprs
          (recur rest-exprs))))))

(defn extract-arity-info [ctx parsed-bodies]
  (reduce (fn [acc {:keys [:fixed-arity :varargs? :min-arity :ret :args :arg-vec
                           :arglist-str]}]
            (let [arg-tags (when (some identity args)
                             args)
                  v (assoc-some {}
                                :ret ret :min-arity min-arity
                                :args arg-tags
                                :arglist-str arglist-str)]
              (if varargs?
                (assoc acc :varargs v)
                (do
                  (when (get acc fixed-arity)
                    (findings/reg-finding! ctx
                                           (node->line
                                            (:filename ctx)
                                            arg-vec
                                            :conflicting-fn-arity
                                            (format "More than one function overload with arity %s."
                                                    fixed-arity))))
                  (assoc acc fixed-arity v)))))
          {}
          parsed-bodies))

(defn analyze-defn
  [ctx expr defined-by defined-by->lint-as]
  (let [ns-name (-> ctx :ns :name)
        ;; "my-fn docstring" {:no-doc true} [x y z] x
        [name-node & children] (next (:children expr))
        name-node-meta-nodes (:meta name-node)
        name-node (when name-node (meta/lift-meta-content2 ctx name-node))
        _ (when name-node (lint-fn-name! ctx name-node))
        _ (utils/handle-ignore ctx name-node)
        fn-name (some-> (:value name-node)
                        (with-meta (meta name-node)))
        call (name (symbol-call expr))
        var-leading-meta (meta name-node)
        _ (when (identical? :clj (:lang ctx))
            (when-let [t (:tag var-leading-meta)]
              (let [tstr (str t)
                    matching-node (some #(when (= tstr (str %))
                                           %) name-node-meta-nodes)]
                (when matching-node
                  (findings/reg-finding! ctx (utils/node->line
                                              (:filename ctx)
                                              matching-node
                                              :non-arg-vec-return-type-hint
                                              (str "Prefer placing return type hint on arg vector: " t)))))))
        docstring (string-from-token (first children))
        doc-node (when docstring (first children))
        children (if docstring (next children) children)
        meta-node (when-let [fc (first children)]
                    (let [t (tag fc)]
                      (when (= :map t) fc)))
        children (if meta-node (next children) children)
        meta-node2 (when-let [fc (first children)]
                     (let [fct (tag fc)]
                       (when (= :list fct)
                         (when-let [lc (last (next children))]
                           (let [lct (tag lc)]
                             (when (= :map lct) lc))))))
        children (if meta-node2 (butlast children) children)
        ;; to not trigger unused-value linter we insert empty vector in callstack
        meta-ctx (update ctx :callstack (fn [cs]
                                          (cons [] cs)))
        ;; use dorun to force evaluation, we don't use the result!
        _ (when meta-node (dorun (analyze-expression** meta-ctx meta-node)))
        _ (when meta-node2 (dorun (analyze-expression** meta-ctx meta-node2)))
        meta-node-meta (when meta-node (sexpr meta-node))
        [doc-node docstring] (or (and meta-node-meta
                                      (:doc meta-node-meta)
                                      (docstring/docs-from-meta meta-node))
                                 [doc-node docstring])
        meta-node2-meta (when meta-node2 (sexpr meta-node2))
        [doc-node docstring] (or (and meta-node2-meta
                                      (:doc meta-node2-meta)
                                      (docstring/docs-from-meta meta-node2))
                                 [doc-node docstring])
        var-meta (if meta-node-meta
                   (merge var-leading-meta meta-node-meta)
                   var-leading-meta)
        var-meta (if meta-node2-meta
                   (merge var-meta meta-node2-meta)
                   var-meta)
        _ (let [fq-sym (symbol (str ns-name) (str fn-name))
                {:clj-kondo/keys [config lint-as ignore]} var-meta
                config (cond-> nil
                         config (assoc-in [:config-in-call fq-sym] (config/unquote config))
                         lint-as (assoc-in [:lint-as fq-sym] (config/unquote lint-as))
                         ignore (assoc-in [:config-in-call fq-sym :ignore] ignore))]
            (when config (swap! (:inline-configs ctx) conj config)))
        macro? (when (or (one-of defined-by->lint-as
                                 [clojure.core/defmacro
                                  cljs.core/defmacro])
                         (:macro var-meta))
                 true)
        deprecated (:deprecated var-meta)
        ctx (if macro?
              (ctx-with-bindings ctx '{&env {}
                                       &form {}})
              ctx)
        private? (or (= "defn-" call)
                     (:private var-meta))
        [doc-node docstring] (if docstring
                               [doc-node docstring]
                               (when (some-> var-leading-meta :doc str)
                                 (some docstring/docs-from-meta name-node-meta-nodes)))
        bodies (fn-bodies ctx children expr)
        _ (when (empty? bodies)
            (findings/reg-finding! ctx
                                   (node->line (:filename ctx)
                                               expr
                                               :syntax
                                               "Invalid function body.")))
        ;; var is known when making recursive call
        _ (when fn-name
            (namespace/reg-var!
             ctx ns-name fn-name expr {:temp true}))
        parsed-bodies (map #(analyze-fn-body
                             (-> ctx
                                 (assoc :docstring docstring
                                        :in-def fn-name
                                        :macro? macro?))
                             %)
                           bodies)
        ;; poor naming, this is for type information
        arities (extract-arity-info ctx parsed-bodies)
        fixed-arities (into #{} (filter number?) (keys arities))
        varargs-min-arity (get-in arities [:varargs :min-arity])]
    (when fn-name
      (namespace/reg-var!
       ctx ns-name fn-name expr
       (assoc-some var-leading-meta
                   :user-meta (when (:analysis-var-meta ctx)
                                (conj (or (:user-meta var-leading-meta) []) meta-node-meta meta-node2-meta))
                   :macro macro?
                   :private private?
                   :deprecated deprecated
                   :defined-by defined-by
                   :defined-by->lint-as defined-by->lint-as
                   :fixed-arities (not-empty fixed-arities)
                   :arglist-strs (when (:analyze-arglists? ctx)
                                   (or (meta-node->arglist-strs meta-node2)
                                       (meta-node->arglist-strs meta-node)
                                       (mapv :arglist-str parsed-bodies)))
                   :arities arities
                   :varargs-min-arity varargs-min-arity
                   :doc docstring
                   :added (:added var-meta)
                   :type (when (one-of defined-by->lint-as
                                       [clojure.core/defn
                                        cljs.core/defn
                                        clojure.core/defn-
                                        cljs.core/defn-])
                           :fn))))
    (docstring/lint-docstring! ctx doc-node docstring)
    (mapcat :parsed parsed-bodies)))

(defn analyze-case [ctx expr]
  (let [children (rest (:children expr))
        matched-val (first children)
        [test-ctx test-opts]
        (if (identical? :cljs (:lang ctx))
          [(utils/ctx-with-linters-disabled ctx [:unresolved-symbol :private-call])
           nil]
          [ctx {:quote? true}])]
    (analyze-expression** ctx matched-val)
    (loop [[constant expr & exprs] (rest children)
           seen-constants #{}]
      (when constant
        (if-not expr
          ;; this is the default expression
          (analyze-expression** ctx constant)
          (let [t (tag constant)
                list-const? (identical? :list t)
                dupe-cands (if list-const? (:children constant) [constant])]
            (when (symbol? (:value constant))
              (findings/reg-finding!
               ctx
               (node->line (:filename ctx) constant :case-symbol-test
                           "Case test symbol is compile time constant and is never evaluated.")))
            (let [t (:tag constant)]
              (when (identical? :quote t)
                (findings/reg-finding!
                 ctx
                 (node->line (:filename ctx) constant :case-quoted-test
                             "Case test is compile time constant and should not be quoted."))))
            (loop [[dupe & more] dupe-cands
                   seen-local seen-constants]
              (let [t (:tag dupe)]
                (when (identical? :quote t)
                  (findings/reg-finding!
                   ctx
                   (node->line (:filename ctx) dupe :case-quoted-test
                               "Case test is compile time constant and should not be quoted."))))
              (when (symbol? (:value dupe))
                (findings/reg-finding!
                 ctx
                 (node->line (:filename ctx) dupe :case-symbol-test
                             "Case test symbol is compile time constant and is never evaluated.")))
              (let [s-dupe (str dupe)]
                (when (seen-local s-dupe)
                  (findings/reg-finding!
                   ctx
                   (node->line (:filename ctx) dupe :case-duplicate-test
                               (format "Duplicate case test constant: %s" s-dupe))))
                (when (seq more)
                  (recur more (conj seen-local s-dupe)))))
            (if list-const?
              (run! #(analyze-usages2 test-ctx % test-opts) (:children constant))
              (analyze-usages2 test-ctx constant test-opts))
            (when expr
              (analyze-expression** ctx expr)
              (recur exprs (into seen-constants (map str dupe-cands))))))))))

(defn expr-bindings [ctx binding-vector scoped-expr]
  (let [ctx (update ctx :callstack conj [:nil :vector])]
    (->> binding-vector :children
         (take-nth 2)
         (map #(extract-bindings ctx % scoped-expr {}))
         (reduce deep-merge {}))))

(defn analyze-let-like-bindings [ctx binding-vector scoped-expr]
  (let [resolved-as-clojure-var-name (:resolved-as-clojure-var-name ctx)
        for-like? (one-of resolved-as-clojure-var-name [for doseq])
        callstack (:callstack ctx)
        call (-> callstack second second)
        let? (= 'let call)
        ;; don't register arg types on the same level
        ctx (assoc ctx :arg-types (atom []))]
    (loop [[binding value & rest-bindings] (-> binding-vector :children)
           bindings (:bindings ctx)
           arities (:arities ctx)
           analyzed []]
      (if binding
        (let [binding-tag (tag binding)
              binding-val (case binding-tag
                            :token (or
                                    ;; symbol
                                    (:value binding)
                                    ;; keyword
                                    (:k binding))
                            nil)
              ;; binding-sexpr (sexpr binding)
              for-let? (and for-like?
                            (= :let binding-val))]
          (if for-let?
            (let [{new-bindings :bindings
                   new-analyzed :analyzed
                   new-arities :arities}
                  (analyze-let-like-bindings
                   (ctx-with-bindings ctx bindings) value scoped-expr)]
              (recur rest-bindings
                     (merge bindings new-bindings)
                     (merge arities new-arities)
                     (concat analyzed new-analyzed)))
            (let [binding (cond for-let? value
                                ;; ignore :when and :while in for
                                (keyword? binding-val) nil
                                :else binding)
                  ctx* (-> ctx
                           (ctx-with-bindings bindings)
                           (update :arities merge arities))
                  value-id (gensym)
                  analyzed-value (when (and value (not for-let?))
                                   (analyze-expression** ctx* (assoc value :id value-id)))
                  tag (when (and let? binding (= :token (tag binding)))
                        (let [maybe-call (get @(:calls-by-id ctx) value-id)]
                          (cond maybe-call (:ret maybe-call)
                                value (types/expr->tag ctx* value))))
                  new-bindings (when binding (extract-bindings ctx* binding scoped-expr {:tag tag}))
                  analyzed-binding (:analyzed new-bindings)
                  new-bindings (dissoc new-bindings :analyzed)
                  m (meta analyzed-value)
                  arity (:arity m)
                  types-by-arity (:arities m)
                  next-arities (if arity
                                 ;; in this case binding-sexpr is a symbol,
                                 ;; since functions cannot be destructured
                                 (assoc arities binding-val (assoc arity
                                                                   :types types-by-arity))
                                 arities)]
              (recur rest-bindings
                     (merge bindings new-bindings)
                     next-arities (concat analyzed analyzed-binding analyzed-value)))))
        {:arities arities
         :bindings bindings
         :analyzed analyzed}))))

(defn lint-even-forms-bindings! [ctx form-name bv]
  (let [num-children (count (:children bv))]
    (when (odd? num-children)
      (findings/reg-finding!
       ctx
       (node->line (:filename ctx) bv :syntax
                   (format "%s binding vector requires even number of forms" form-name))))))

(defn assert-vector [ctx call expr]
  (when expr
    (let [vec? (and expr (= :vector (tag expr)))]
      (if-not vec?
        (do (findings/reg-finding!
             ctx
             (node->line (:filename ctx) expr :syntax
                         ;; cf. error in clojure
                         (format "%s requires a vector for its binding" call)))
            nil)
        expr))))

(defn analyze-like-let
  [{:keys [:filename :callstack
           :let-parent] :as ctx} expr]
  (let [call (-> callstack first second)
        [current-call parent-call] callstack
        parent-let (one-of parent-call
                           [[clojure.core let]
                            [cljs.core let]])
        current-let (one-of current-call
                            [[clojure.core let]
                             [cljs.core let]])
        bv-node (-> expr :children second)
        valid-bv-node (assert-vector ctx call bv-node)]
    (when (and current-let
               (not (:clj-kondo.impl/generated expr))
               (or (and parent-let (= parent-let let-parent)
                        ;; not generated by hook code
                        (not (:clj-kondo.impl/generated (meta parent-let))))
                   (and valid-bv-node (empty? (:children valid-bv-node)))))
      (findings/reg-finding!
       ctx
       (node->line filename expr :redundant-let "Redundant let expression.")))
    (when bv-node
      (let [{analyzed-bindings :bindings
             arities :arities
             analyzed :analyzed}
            (analyze-let-like-bindings
             (-> ctx
                 ;; prevent linting redundant let when using let in bindings
                 (update :callstack #(cons [nil :let-bindings] %))) valid-bv-node expr)
            let-body (nnext (:children expr))
            let-parent (when (and current-let (= 1 (count let-body)))
                         current-let)
            _ (lint-even-forms-bindings! ctx call valid-bv-node)
            analyzed (concat analyzed
                             (doall
                              (analyze-children
                               (-> ctx
                                   (ctx-with-bindings analyzed-bindings)
                                   (update :arities merge arities)
                                   (assoc :let-parent let-parent))
                               let-body
                               false)))]
        analyzed))))

(defn analyze-do [{:keys [:filename :callstack] :as ctx} expr]
  (let [parent-call (second callstack)
        core? (one-of (first parent-call) [clojure.core cljs.core])
        core-sym (when core?
                   (second parent-call))
        ;; avoid warnings from hook code
        generated? (:clj-kondo.impl/generated expr)
        redundant?
        (and (not generated?)
             (not= 'fn* core-sym)
             (not= 'let* core-sym)
             (or
              ;; zero or one children
              (< (count (rest (:children expr))) 2)
              (and core?
                   (not (:clj-kondo.impl/generated (meta parent-call)))
                   (or
                    ;; explicit do
                    (= 'do core-sym)
                    ;; implicit do
                    (one-of core-sym [fn defn defn-
                                      let when-let loop binding with-open
                                      doseq try when when-not when-first
                                      when-some future])))))]
    (when redundant?
      (findings/reg-finding!
       ctx
       (node->line filename expr :redundant-do "redundant do"))))
  (analyze-children ctx (next (:children expr)) false))

(defn lint-two-forms-binding-vector! [ctx form-name expr]
  (let [num-children (count (:children expr))]
    (when (not= 2 num-children)
      (findings/reg-finding!
       ctx
       (node->line (:filename ctx) expr :syntax (format "%s binding vector requires exactly 2 forms" form-name))))))

(defn analyze-conditional-let [ctx call expr]
  (let [children (next (:children expr))
        bv (first children)
        vec? (when bv (= :vector (tag bv)))]
    (when vec?
      (let [if? (one-of call [if-let if-some])
            condition (-> bv :children second)
            body-exprs (next children)
            bindings (expr-bindings ctx bv (if if? (first body-exprs) expr))
            ctx-with-binding (ctx-with-bindings ctx
                                                (dissoc bindings
                                                        :analyzed))]
        (lint-two-forms-binding-vector! ctx call bv)
        (concat (:analyzed bindings)
                (analyze-expression** (update ctx :callstack conj [:vector]) condition)
                (if if?
                  ;; in the case of if, the binding is only valid in the first expression
                  (concat
                   (analyze-expression** (ctx-with-bindings ctx
                                                            (dissoc bindings
                                                                    :analyzed))
                                         (first body-exprs))
                   (analyze-children ctx (rest body-exprs) false))
                  (analyze-children ctx-with-binding body-exprs false)))))))

(defn fn-arity [ctx bodies]
  (let [arities (map #(analyze-fn-arity ctx %) bodies)
        fixed-arities (set (keep (comp :fixed-arity :arity) arities))
        varargs-min-arity (some #(when (:varargs? (:arity %))
                                   (:min-arity (:arity %))) arities)
        arglist-strs (vec (keep :arglist-str arities))
        bodies (map (fn [body arity]
                      (assoc body :analyzed-arity arity))
                    bodies arities)]
    (cond->
        ;; we return bodies so we don't have to run fn-arity twice over the bodies
        {:bodies bodies}
      (seq fixed-arities) (assoc :fixed-arities fixed-arities)
      varargs-min-arity (assoc :varargs-min-arity varargs-min-arity)
      (seq arglist-strs) (assoc :arglist-strs arglist-strs))))

(defn- def? [x]
  (one-of x [[clojure.core def] [cljs.core def]]))

(defn- let? [x]
  (one-of x [[clojure.core let] [cljs.core let]]))

(defn- def-fn? [{:keys [callstack]}]
  (let [[_ parent extra-parent] callstack]
    (or (def? parent)
        (and (let? parent) (def? extra-parent)))))

(defn- reg-def-fn! [ctx expr filename]
  (findings/reg-finding!
   ctx
   (node->line
    filename
    expr
    :def-fn
    "Use defn instead of def + fn")))

(defn analyze-fn [ctx expr]
  (let [ctx (assoc ctx :seen-recur? (volatile! nil))
        protocol-fn (:protocol-fn expr)
        ctx (assoc ctx :protocol-fn protocol-fn)
        children (:children expr)
        ?name-expr (second children)
        ?fn-name (when ?name-expr
                   (when-let [n (utils/symbol-from-token ?name-expr)]
                     n))
        _ (when (and ?name-expr (identical? :token (utils/tag ?name-expr)))
            (lint-fn-name! ctx ?name-expr))
        bodies (fn-bodies ctx (next children) expr)
        ;; we need the arity beforehand because this is valid in each body
        arity (fn-arity ctx bodies)
        bodies (:bodies arity)
        filename (:filename ctx)
        parsed-bodies
        (let [ctx (-> ctx
                      (assoc :fn-body-count (count bodies))
                      (assoc :fn-parent-loc (meta expr)))]
          (map #(analyze-fn-body
                 (if ?fn-name
                   (-> ctx
                       (update :bindings conj [?fn-name
                                               (assoc (meta ?name-expr)
                                                      :name ?fn-name
                                                      :filename filename)])
                       (update :arities assoc ?fn-name
                               arity))
                   ctx) %) bodies))
        arities
        (when-not (some-> ctx :def-meta :macro)
          (extract-arity-info ctx parsed-bodies))
        fixed-arities (when arities (into #{} (filter number?) (keys arities)))
        varargs-min-arity (when arities (get-in arities [:varargs :min-arity]))
        arglist-strs (when arities (into [] (keep :arglist-str) (vals arities)))
        parsed-bodies (mapcat :parsed parsed-bodies)]
    (when (and (not (linter-disabled? ctx :def-fn))
               (def-fn? ctx))
      (reg-def-fn! ctx expr filename))
    (with-meta parsed-bodies
      (when arities
        (cond-> {:arity {:fixed-arities fixed-arities
                         :varargs-min-arity varargs-min-arity}
                 :arities arities}
          (seq arglist-strs) (assoc-in [:arity :arglist-strs] arglist-strs))))))

(defn analyze-alias [ctx expr]
  (let [ns (:ns ctx)
        [alias-expr ns-expr :as children] (rest (:children expr))
        alias-sym
        (let [t (tag alias-expr)]
          (or (when (identical? :quote t)
                (:value (first (:children alias-expr))))
              (when (identical? :list t)
                (let [children (:children alias-expr)]
                  (when (= 'quote (some-> children first
                                          utils/symbol-from-token))
                    (utils/symbol-from-token (second children)))))))
        ns-sym
        (let [t (tag ns-expr)]
          (or (when (identical? :quote t)
                (:value (first (:children ns-expr))))
              (when (identical? :list t)
                (let [children (:children ns-expr)]
                  (when (= 'quote (some-> children first
                                          utils/symbol-from-token))
                    (utils/symbol-from-token (second children)))))))]
    (if (and alias-sym (symbol? alias-sym) ns-sym (symbol? ns-sym))
      (namespace/reg-alias! ctx (:name ns) alias-sym ns-sym)
      (analyze-children ctx children))
    (assoc-in ns [:qualify-ns alias-sym] ns-sym)))

(defn analyze-loop [ctx expr]
  (let [seen-recur? (volatile! nil)
        ctx (-> (assoc ctx :seen-recur? seen-recur?)
                (dissoc ctx :protocol-fn))
        bv (-> expr :children second)]
    (when (and bv (= :vector (tag bv)))
      (let [arg-count (let [c (count (:children bv))]
                        (when (even? c)
                          (/ c 2)))
            analyzed (analyze-like-let (assoc ctx
                                              :recur-arity {:fixed-arity arg-count}) expr)]
        (when-not @seen-recur?
          (findings/reg-finding! ctx
                                 (node->line
                                  (:filename ctx)
                                  expr
                                  :loop-without-recur "Loop without recur.")))
        analyzed))))

(defn first-callstack-elt-ignoring-macros
  [callstack]
  (loop [callstack callstack]
    (when-let [cse (first callstack)]
      (if (not (one-of (second cse)
                       [-> ->> some-> some->> doto cond->
                        alt! alt!!]))
        cse
        (recur (rest callstack))))))

(defn analyze-recur [ctx expr]
  (let [filename (:filename ctx)
        recur-arity (:recur-arity ctx)
        seen-recur? (:seen-recur? ctx)]
    (when seen-recur? (vreset! seen-recur? true))
    (when-not (or (linter-disabled? ctx :invalid-arity)
                  (config/skip? (:config ctx) :invalid-arity (:callstack ctx)))
      (let [arg-count (count (rest (:children expr)))
            expected-arity
            (or (:fixed-arity recur-arity)
                ;; varargs must be passed as a seq or nil in recur
                (when-let [min-arity (:min-arity recur-arity)]
                  (inc min-arity)))
            expected-arity (if (:protocol-fn ctx)
                             ;; compensate for this argument
                             (dec expected-arity)
                             expected-arity)]
        (let [len (:len ctx)
              idx (:idx ctx)
              parent (-> (:callstack ctx)
                         rest first-callstack-elt-ignoring-macros second)]
          (when (and len idx
                     (not= (dec len) idx)
                     (not (one-of parent [if case cond if-let if-not if-some condp])))
            (findings/reg-finding!
             ctx
             (node->line
              filename
              expr
              :unexpected-recur "Recur can only be used in tail position."))))
        (cond
          (not expected-arity)
          (findings/reg-finding!
           ctx
           (node->line
            filename
            expr
            :unexpected-recur "Unexpected usage of recur."))
          (not= expected-arity arg-count)
          (findings/reg-finding!
           ctx
           (node->line
            filename
            expr
            :invalid-arity
            (format "recur argument count mismatch (expected %d, got %d)" expected-arity arg-count)))
          :else nil))))
  (analyze-children ctx (rest (:children expr))))

(defn analyze-letfn [ctx expr]
  (let [fns (-> expr :children second :children)
        name-exprs (map #(-> % :children first) fns)
        bindings (when (seq name-exprs)
                   (mapv (fn [name-expr]
                           (let [v (cond-> (assoc (meta name-expr)
                                                  :name (:value name-expr)
                                                  :filename (:filename ctx))
                                     (:analyze-locals? ctx)
                                     (-> (assoc :id (swap! (:id-gen ctx) inc)
                                                :str (:str name-expr))
                                         (merge (scope-end expr))))]
                             (namespace/reg-binding! ctx (-> ctx :ns :name) v)
                             [(:value name-expr) v]))
                         name-exprs))
        ctx (ctx-with-bindings ctx bindings)
        protocol-fn (:protocol-fn ctx)
        ctx* (dissoc ctx :protocol-fn)
        processed-fns (for [f fns
                            :let [children (:children f)
                                  fn-name (:value (first children))
                                  bodies (fn-bodies ctx* (next children) f)
                                  arity (fn-arity ctx bodies)
                                  bodies (:bodies arity)]]
                        {:name fn-name
                         :arity arity
                         :bodies bodies})
        ctx* (reduce (fn [ctx pf]
                       (assoc-in ctx [:arities (:name pf)]
                                 (:arity pf)))
                     ctx* processed-fns)
        parsed-fns (map #(analyze-fn-body ctx* %) (mapcat :bodies processed-fns))
        ctx (assoc ctx* :protocol-fn protocol-fn)
        analyzed-children (analyze-children ctx (->> expr :children (drop 2)))]
    (concat (mapcat :parsed parsed-fns) analyzed-children)))

(declare analyze-defmethod)

(defn current-namespace-var-name [ctx var-name-node var-name]
  (if (qualified-symbol? var-name)
    (let [ns (:ns ctx)
          alias (symbol (namespace var-name))]
      (if (= (:name ns) (get (:qualify-ns ns) alias))
        (symbol (name var-name))
        (do (findings/reg-finding!
             ctx
             (node->line (:filename ctx) var-name-node
                         :syntax
                         (str "Invalid var name: " var-name)))
            nil)))
    (some-> var-name
            (with-meta (meta var-name-node)))))

(defn analyze-def [ctx expr defined-by defined-by->lint-as]
  (let [children (next (:children expr))
        raw-var-name-node (first children)
        var-name-node-meta-nodes (:meta raw-var-name-node)
        _ (utils/handle-ignore ctx raw-var-name-node)
        var-name-node (meta/lift-meta-content2 ctx raw-var-name-node)
        metadata (meta var-name-node)
        var-name (:value var-name-node)
        var-name (current-namespace-var-name ctx var-name-node var-name)
        children (next children)
        docstring (when (> (count children) 1)
                    (string-from-token (first children)))
        defmulti? (or (= 'clojure.core/defmulti defined-by->lint-as)
                      (= 'cljs.core/defmulti defined-by->lint-as))
        doc-node (when docstring
                   (first children))
        [child & children] (if docstring (next children) children)
        core-def? (one-of (first (:callstack ctx)) [[clojure.core def] [cljs.core def]])
        _ (when (and core-def? children)
            (findings/reg-finding! ctx (utils/node->line (:filename ctx) expr :invalid-arity "Too many arguments to def")))
        _ (when-not child
            (findings/reg-finding! ctx (utils/node->line (:filename ctx) expr :uninitialized-var "Uninitialized var")))
        [extra-meta extra-meta-node children] (if (and defmulti?
                                                       child
                                                       (identical? :map (utils/tag child)))
                                                [(sexpr child) child children]
                                                [nil nil (cons child children)])
        metadata (if extra-meta (merge metadata extra-meta)
                     metadata)
        [doc-node docstring] (or (and extra-meta
                                      (:doc extra-meta)
                                      (docstring/docs-from-meta extra-meta-node))
                                 [doc-node docstring])
        [doc-node docstring] (if docstring
                               [doc-node docstring]
                               (when (some-> metadata :doc str)
                                 (some docstring/docs-from-meta var-name-node-meta-nodes)))
        ctx (assoc ctx :in-def var-name :def-meta metadata :defmulti? defmulti?)
        children (if (:analyze-var-defs-shallowly? ctx)
                   []
                   children)
        def-init (when (and core-def?
                            (= 1 (count children)))
                   (or (analyze-expression** ctx (first children))
                       ;; prevent analysis of only child more than once
                       []))
        init-meta (some-> def-init meta)
        ;; :args and :ret is are the type related keys
        ;; together this is called :arities in reg-var!
        arity (when init-meta (:arity init-meta))
        var-name-str (str var-name)
        earmuffed? (re-matches #"\*+[^\*]+\*+" var-name-str)
        dynamic? (:dynamic metadata)]
    (if dynamic?
      (when (not earmuffed?)
        (findings/reg-finding!
         ctx
         (utils/node->line (:filename ctx) var-name-node
                           :dynamic-var-not-earmuffed
                           (str "Var is declared dynamic but name is not earmuffed: " var-name-str))))
      (when earmuffed?
        (findings/reg-finding! ctx
                               (utils/node->line (:filename ctx) var-name-node
                                                 :earmuffed-var-not-dynamic
                                                 (str "Var has earmuffed name but is not declared dynamic: " var-name-str)))))
    (when var-name
      (let [type (when-not dynamic?
                   (some-> (:arg-types ctx) deref first :tag))]
        (namespace/reg-var! ctx (-> ctx :ns :name)
                            var-name
                            expr
                            (assoc-some metadata
                                        :user-meta (when (:analysis-var-meta ctx)
                                                     (conj (:user-meta metadata) extra-meta))
                                        :doc docstring
                                        :defined-by defined-by
                                        :defined-by->lint-as defined-by->lint-as
                                        :fixed-arities (:fixed-arities arity)
                                        :arglist-strs (when (:analyze-arglists? ctx)
                                                        ;; match Clojure behaviour for var name meta nodes,
                                                        ;; it will pick first :arglists, e.g.:
                                                        ;; ^{:arglists '([x])} ^{:arglists '([y])} z
                                                        (or (some meta-node->arglist-strs var-name-node-meta-nodes)
                                                            (meta-node->arglist-strs extra-meta-node)
                                                            (:arglist-strs arity)))
                                        :varargs-min-arity (:varargs-min-arity arity)
                                        :arities (:arities init-meta)
                                        :type type))))
    (docstring/lint-docstring! ctx doc-node docstring)
    (when-not def-init
      ;; this was something else than core/def
      (analyze-children ctx
                        children))))

(declare analyze-defrecord)

(defn analyze-schema [ctx fn-sym expr defined-by defined-by->lint-as]
  (let [{:keys [:expr :schemas]}
        (schema/expand-schema ctx
                              fn-sym
                              expr)]
    (concat
     (case fn-sym
       fn (analyze-fn ctx expr)
       def (analyze-def ctx expr defined-by defined-by->lint-as)
       defn (analyze-defn ctx expr defined-by defined-by->lint-as)
       defmethod (analyze-defmethod ctx expr)
       defrecord
       (analyze-defrecord ctx expr defined-by defined-by->lint-as))
     (analyze-children ctx schemas))))

(defn arity-match? [fixed-arities varargs-min-arity arg-count]
  (or (contains? fixed-arities arg-count)
      (and varargs-min-arity (>= arg-count varargs-min-arity))))

(defn redundant-fn-wrapper [ctx callstack children interop?]
  (when-let [fn-args (:fn-args ctx)]
    (when (and
           (not (:pre-post-map ctx))
           (not (identical? :off (-> ctx :config :linters :redundant-fn-wrapper :level)))
           (not (:extend-type ctx))
           (not interop?)
           (= 1 (:fn-body-count ctx))
           (= 1 (:body-children-count ctx))
           (= (count children) (count fn-args))
           (one-of (first callstack) [[clojure.core fn]
                                      [clojure.core fn*]
                                      [cljs.core fn]
                                      [cljs.core fn*]])
           (not= '[cljs.core .] (second callstack))
           (= (map #(str/replace % #"^%$" "%1") children)
              (map str fn-args)))
      (:fn-parent-loc ctx))))

(defn analyze-binding-call [ctx fn-name binding expr]
  (let [callstack (:callstack ctx)
        config (:config ctx)
        ns-name (-> ctx :ns :name)
        fn-meta (meta fn-name)
        arg-types (atom [])
        ctx (assoc ctx :arg-types arg-types)
        children (:children expr)
        binding-info (get (:arities ctx) fn-name)]
    (when-let [k (types/keyword binding)]
      (when-not (types/match? k :ifn)
        (findings/reg-finding! ctx (node->line (:filename ctx) expr
                                               :type-mismatch
                                               (format "%s cannot be called as a function."
                                                       (str/capitalize (types/label k)))))))
    (namespace/reg-used-binding! ctx
                                 ns-name
                                 binding
                                 (assoc (meta expr)
                                        :name-row (:row fn-meta)
                                        :name-col (:col fn-meta)
                                        :name-end-row (:end-row fn-meta)
                                        :name-end-col (:end-col fn-meta)))
    (when-not (config/skip? config :invalid-arity callstack)
      (let [filename (:filename ctx)]
        (when-not (linter-disabled? ctx :invalid-arity)
          (when-let [{:keys [:fixed-arities :varargs-min-arity]}
                     binding-info]
            ;; (prn :arities types)
            (let [arg-count (count (rest children))]
              (when-not (arity-match? fixed-arities varargs-min-arity arg-count)
                (findings/reg-finding! ctx
                                       (node->line filename expr
                                                   :invalid-arity
                                                   (linters/arity-error nil fn-name arg-count fixed-arities varargs-min-arity)))))))))
    (when-let [fn-parent-loc (redundant-fn-wrapper ctx callstack (rest children) false)]
      (findings/reg-finding!
       ctx
       (assoc fn-parent-loc
              :filename (:filename ctx)
              :type :redundant-fn-wrapper
              :message "Redundant fn wrapper")))
    (let [types (:types binding-info)
          children (rest children)
          type (get types (count children))
          ret (:ret type)]
      (analyze-children (update ctx :callstack conj [nil fn-name]) children false)
      {:ret ret})))

(defn lint-inline-def! [ctx expr]
  (when (:in-def ctx)
    (findings/reg-finding!
     ctx
     (node->line (:filename ctx) expr :inline-def "inline def"))))

(defn analyze-declare [ctx expr defined-by defined-by->lint-as]
  (let [ns-name (-> ctx :ns :name)
        var-name-nodes (next (:children expr))
        var-names (keep (fn [var-name-node]
                          (let [var-sym (->> var-name-node (meta/lift-meta-content2 ctx) :value)]
                            (current-namespace-var-name ctx var-name-node var-sym)))
                        var-name-nodes)]
    (doseq [var-name var-names]
      (let [var-name-meta (meta var-name)]
        (namespace/reg-var! ctx ns-name
                            var-name
                            expr
                            (assoc (meta expr)
                                   :name-row (:row var-name-meta)
                                   :name-col (:col var-name-meta)
                                   :name-end-row (:end-row var-name-meta)
                                   :name-end-col (:end-col var-name-meta)
                                   :declared true
                                   :defined-by defined-by
                                   :defined-by->lint-as defined-by->lint-as))))))

(defn analyze-catch [ctx expr]
  (let [ctx (update ctx :callstack conj [nil 'catch])
        [class-expr binding-expr & exprs] (next (:children expr))
        _ (analyze-expression** ctx class-expr) ;; analyze usage for unused import linter
        binding (extract-bindings ctx binding-expr (last exprs) {})]
    (analyze-children (ctx-with-bindings ctx binding)
                      exprs)))

(defn analyze-try [ctx expr]
  (let [children (next (:children expr))
        children-until-catch-or-finally
        (take-while #(let [sc (symbol-call %)]
                       (and (not= 'catch sc)
                            (not= 'finally sc))) children)
        cnt (count children-until-catch-or-finally)
        children-after (drop cnt children)]
    (analyze-children ctx children-until-catch-or-finally)
    (loop [[fst-child & rst-children] children-after
           analyzed []
           ;; TODO: lint syntax
           _catch-phase false
           _finally-phase false
           has-catch-or-finally? false]
      (if fst-child
        (case (symbol-call fst-child)
          catch
          (let [analyzed-catch (analyze-catch ctx fst-child)]
            (recur rst-children (into analyzed analyzed-catch)
                   true false true))
          finally
          (recur
           rst-children
           (into analyzed (analyze-children (update ctx :callstack conj [nil 'finally])
                                            (next (:children fst-child))))
           false false true)
          ;; TODO: should never get here, probably syntax error
          (recur
           rst-children
           (into analyzed (analyze-expression** ctx fst-child))
           false false has-catch-or-finally?))
        (do
          (when-not has-catch-or-finally?
            (findings/reg-finding!
             ctx
             (node->line
              (:filename ctx)
              expr
              :missing-clause-in-try
              "Missing catch or finally in try")))
          analyzed)))))

(defn analyze-defprotocol [{:keys [ns] :as ctx} expr defined-by defined-by->lint-as]
  ;; for syntax, see https://clojure.org/reference/protocols#_basics
  (let [children (next (:children expr))
        name-node (first children)
        name-node (meta/lift-meta-content2 ctx name-node)
        name-meta (meta name-node)
        protocol-name (:value name-node)
        ns-name (:name ns)
        docstring (string-from-token (second children))
        doc-node (when docstring
                   (second children))
        transduce-arity-vecs (filter
                              ;; skip last docstring
                              #(when (= :vector (tag %)) %))]
    (when protocol-name
      (namespace/reg-var! ctx ns-name protocol-name expr
                          (assoc-some name-meta
                                      :user-meta (when (:analysis-var-meta ctx)
                                                   (:user-meta name-meta))
                                      :doc docstring
                                      :defined-by defined-by
                                      :defined-by->lint-as defined-by->lint-as)))
    (docstring/lint-docstring! ctx doc-node docstring)
    (doseq [c (next children)
            :when (= :list (tag c)) ;; skip first docstring
            :let [children (:children c)
                  name-node (first children)
                  name-node (meta/lift-meta-content2 ctx name-node)
                  name-meta (meta name-node)
                  fn-name (:value name-node)
                  arities (rest children)
                  docstring (string-from-token (last children))
                  doc-node (when docstring
                             (last children))]]
      ;; This is here for analyzing usages of type hints, but it also causes
      ;; false positives in the analysis, so we can improve this
      (let [ctx (utils/ctx-with-linter-disabled ctx :unresolved-symbol)]
        (run! #(analyze-usages2 ctx %) arities))
      (when fn-name
        (let [arglist-strs (when (:analyze-arglists? ctx)
                             (->> arities
                                  (into [] (comp transduce-arity-vecs (map str)))
                                  (not-empty)))
              fixed-arities (into #{}
                                  (comp transduce-arity-vecs
                                        (map #(let [children (:children %)]
                                                (run! (fn [child]
                                                        (when (= '& (:value child))
                                                          (findings/reg-finding!
                                                           ctx
                                                           (node->line
                                                            (:filename ctx)
                                                            child
                                                            :protocol-method-varargs
                                                            "Protocol methods do not support varargs."))))
                                                      children)
                                                (count children))))
                                  arities)]
          (utils/handle-ignore ctx c)
          (namespace/reg-var!
           (cond-> ctx
             (= 'clojure.core/definterface defined-by->lint-as)
             (assoc :skip-reg-var true)) ns-name fn-name c
           (assoc-some (merge name-meta (meta c))
                       :user-meta (when (:analysis-var-meta ctx)
                                    (:user-meta name-meta))
                       :doc docstring
                       :arglist-strs arglist-strs
                       :name-row (:row name-meta)
                       :name-col (:col name-meta)
                       :name-end-row (:end-row name-meta)
                       :name-end-col (:end-col name-meta)
                       :fixed-arities fixed-arities
                       :protocol-ns ns-name
                       :protocol-name protocol-name
                       :defined-by defined-by
                       :defined-by->lint-as defined-by->lint-as))
          (docstring/lint-docstring! ctx doc-node docstring))))))

(defn analyze-protocol-impls [ctx defined-by defined-by->lint-as ns-name children]
  (let [def-by (name defined-by)]
    (loop [current-protocol nil
           children children
           protocol-ns nil
           protocol-name nil]
      (when-first [c children]
        (if-let [sym (utils/symbol-from-token c)]
          ;; We have encountered a protocol or interface name, or a
          ;; record or type name (in the case of extend-protocol and
          ;; extend-type). We need to deal with extend-protocol in a
          ;; special way, as there is a single protocol being extented
          ;; to multiple records/types.
          (do
            (when-not (and (identical? :cljs (:lang ctx))
                           (= 'Object sym))
              (analyze-expression** ctx c))
            (recur (case (name defined-by)
                     "extend-protocol" (if (nil? current-protocol)
                                         ;; extend-protocol has the protocol name as
                                         ;; it first symbol
                                         sym
                                         ;; but has record/type names in its body that
                                         ;; we need to ignore, and keep the initial
                                         ;; (and only) protocol name.
                                         current-protocol)
                     "extend-type" (if (nil? current-protocol)
                                     ;; extend-type has a type name as it first symbol,
                                     ;; not a protocol name. We need to skip it.
                                     (utils/symbol-from-token (second children))
                                     sym)
                     ;; The rest of the use cases have only protocol names in their body.
                     sym)
                   (rest children) protocol-ns protocol-name))
          ;; Assume protocol fn impl. Analyzing the fn sym can cause false
          ;; positives. We are passing it to analyze-fn as is, so (foo [x y z])
          ;; is linted as (fn [x y z])
          (let [fn-children (:children c)
                protocol-method-name (first fn-children)]
            (when (and current-protocol
                       (not= "definterface" def-by))
              (let [[protocol-ns protocol-name]
                    (if (or (not= "extend-protocol" def-by)
                            (not protocol-ns))
                      (let [{pns :ns pname :name} (resolve-name ctx true ns-name current-protocol nil)]
                        [pns pname])
                      ;; we already have the resolved ns + name for extend-protocol
                      [protocol-ns protocol-name])]
                (analysis/reg-protocol-impl! ctx
                                             (:filename ctx)
                                             ns-name
                                             protocol-ns
                                             protocol-name
                                             c
                                             protocol-method-name
                                             defined-by
                                             defined-by->lint-as)))
            ;; protocol-fn-name might contain metadata
            (meta/lift-meta-content2 ctx protocol-method-name)
            (utils/handle-ignore ctx c)
            (analyze-fn (update ctx :callstack #(cons [nil :protocol-method] %))
                        (assoc c :protocol-fn (and (not= "extend-protocol" def-by)
                                                   (not= "extend-type" def-by))))
            (recur current-protocol (rest children) protocol-ns protocol-name)))))))

(defn analyze-defrecord
  "Analyzes defrecord and deftype."
  [{:keys [ns lang] :as ctx} expr defined-by defined-by->lint-as]
  (let [ns-name (:name ns)
        children (:children expr)
        children (next children)
        name-node (first children)
        name-node (meta/lift-meta-content2 ctx name-node)
        metadata (meta name-node)
        metadata (assoc metadata :defined-by defined-by :defined-by->lint-as defined-by->lint-as)
        record-name (:value name-node)
        binding-vector (second children)
        field-count (count (:children binding-vector))
        bindings (extract-bindings (assoc ctx
                                          :mark-bindings-used? true)
                                   binding-vector
                                   expr
                                   {})
        arglists? (:analyze-arglists? ctx)
        ctx (ctx-with-bindings ctx bindings)]
    (namespace/reg-var! ctx ns-name record-name expr (cond-> metadata
                                                       (identical? :clj lang) (assoc :class true)))
    (namespace/reg-imports! ctx ns-name {(with-meta record-name
                                           {:clj-kondo/mark-used true}) ns-name})
    (when-not (identical? :off (-> ctx :config :linters :duplicate-field :level))
      (doseq [[_ fields] (group-by identity (:children binding-vector))]
        (when (> (count fields) 1)
          (doseq [field fields]
            (findings/reg-finding!
             ctx
             (node->line (:filename ctx) field
                         :duplicate-field
                         (format "Duplicate field name: %s" (:value field))))))))
    (namespace/reg-var! ctx ns-name (symbol (str "->" record-name)) expr
                        (assoc-some metadata
                                    :arglist-strs (when arglists?
                                                    [(str binding-vector)])
                                    :fixed-arities #{field-count}))
    (when (= "defrecord" (name defined-by->lint-as))
      (namespace/reg-var! ctx ns-name (symbol (str "map->" record-name))
                          expr (assoc-some metadata
                                           :arglist-strs (when arglists?
                                                           ["[m]"])
                                           :fixed-arities #{1})))
    (analyze-protocol-impls ctx defined-by defined-by->lint-as ns-name (nnext children))))

(defn analyze-defmethod [ctx expr]
  (let [children (next (:children expr))
        [method-name-node dispatch-val-node & fn-tail] children
        _ (analyze-usages2 (assoc ctx
                                  :defmethod true,
                                  :dispatch-val-str (pr-str (sexpr dispatch-val-node)))
                           method-name-node)
        _ (analyze-expression** ctx dispatch-val-node)]
    (analyze-fn ctx {:children (cons nil fn-tail)})))

(defn analyze-areduce [ctx expr]
  (let [children (next (:children expr))
        [array-expr index-binding-expr ret-binding-expr init-expr body] children
        index-binding (extract-bindings ctx index-binding-expr expr {})
        ret-binding (extract-bindings ctx ret-binding-expr expr {})
        bindings (merge index-binding ret-binding)
        analyzed-array-expr (analyze-expression** ctx array-expr)
        analyzed-init-expr (analyze-expression** ctx init-expr)
        analyzed-body (analyze-expression** (ctx-with-bindings ctx bindings) body)]
    (concat analyzed-array-expr analyzed-init-expr analyzed-body)))

(defn analyze-this-as [ctx expr]
  (let [[binding-expr & body-exprs] (next (:children expr))
        binding (extract-bindings ctx binding-expr expr {})]
    (analyze-children (ctx-with-bindings ctx binding)
                      body-exprs)))

(defn analyze-as-> [ctx expr]
  (let [children (next (:children expr))
        [as-expr name-expr & forms-exprs] children
        analyzed-as-expr (analyze-expression** ctx as-expr)
        binding (extract-bindings ctx name-expr expr {})]
    (concat analyzed-as-expr
            (analyze-children (ctx-with-bindings ctx binding)
                              forms-exprs))))

(defn analyze-memfn [ctx expr]
  (analyze-children (utils/ctx-with-linter-disabled ctx :unresolved-symbol)
                    (next (:children expr))))

(defn analyze-empty?
  [ctx expr]
  (let [cs (:callstack ctx)
        not-expr (one-of (second cs) [[clojure.core not] [cljs.core not]])]
    (when not-expr
      (findings/reg-finding!
       ctx
       (node->line (:filename ctx) not-expr
                   :not-empty?
                   "use the idiom (seq x) rather than (not (empty? x))")))
    (analyze-children ctx (rest (:children expr)) false)))

(defn analyze-import-libspec [ctx ns-name expr]
  (let [libspec-expr (if (= :quote (tag expr))
                       (first (:children expr))
                       expr)
        analyzed (namespace-analyzer/analyze-import ctx ns-name libspec-expr)]
    (namespace/reg-imports! ctx ns-name analyzed)))

(defn analyze-import
  [ctx expr]
  (let [ns-name (-> ctx :ns :name)
        children (next (:children expr))]
    (run! #(analyze-import-libspec ctx ns-name %) children)))

(defn analyze-if
  "Analyzes if special form for arity errors"
  [ctx expr]
  (let [args (rest (:children expr))]
    (when-let [[expr msg linter]
               (case (count args)
                 (0 1) [expr "Too few arguments to if." :syntax]
                 (2 3) nil
                 [expr "Too many arguments to if." :syntax])]
      (findings/reg-finding!
       ctx
       (node->line (:filename ctx) expr
                   linter
                   msg)))
    (let [[condition & args] args]
      (when condition
        (analyze-expression** ctx (assoc condition :condition true)))
      (analyze-children ctx args false))))

(defn analyze-constructor
  "Analyzes (new Foo ...) constructor call."
  [ctx expr]
  (let [[_ ctor-node & children] (:children expr)]
    (analyze-expression** (assoc ctx
                                 :constructor-expr expr) ctor-node)
    (analyze-children ctx children)))

(defn analyze-set!
  [ctx expr]
  (let [children (next (:children expr))]
    (if (and (identical? :cljs (:lang ctx))
             (= 3 (count children)))
      ;; ignore second argument which is the field, e.g. (set! o -x 3)
      (analyze-children ctx (cons (first children) (nnext children)))
      (analyze-children ctx children))))

(defn analyze-with-redefs
  [ctx expr]
  (let [call (-> (:callstack ctx) first second) ;; can be with-redefs or binding
        children (next (:children expr))
        binding-vector (assert-vector ctx call (first children))
        _ (when binding-vector
            (lint-even-forms-bindings! ctx call binding-vector))
        bindings (:children binding-vector)
        lhs (take-nth 2 bindings)
        rhs (take-nth 2 (rest bindings))
        body (next children)]
    ;;  NOTE: because of lazy evaluation we need to use dorun!
    (let [ctx (update ctx :callstack conj [nil :vector])]
      (dorun (analyze-children (utils/ctx-with-linter-disabled ctx :private-call)
                               lhs))
      (dorun (analyze-children ctx rhs)))
    (analyze-children ctx body)))

(defn analyze-def-catch-all [ctx expr]
  (let [ns-name (-> ctx :ns :name)
        children (next (:children expr))
        name-expr (->> (first children)
                       (meta/lift-meta-content2 ctx))
        name-sym (:value name-expr)
        body (next children)
        ctx (utils/ctx-with-linters-disabled ctx [:invalid-arity
                                                  :unresolved-symbol
                                                  :unresolved-namespace
                                                  :type-mismatch
                                                  :private-call
                                                  :missing-docstring])]
    (when name-sym (namespace/reg-var! ctx ns-name name-sym expr (meta name-expr)))
    (run! #(analyze-usages2 ctx %) body)))

(defn analyze-when [ctx expr]
  (let [children (next (:children expr))
        condition (assoc (first children) :condition true)
        body (next children)]
    (dorun (analyze-expression**
            ;; avoid redundant do check for condition
            (update ctx :callstack conj nil)
            condition))
    (if-not (seq body)
      (findings/reg-finding!
       ctx
       (node->line
        (:filename ctx)
        expr
        :missing-body-in-when
        "Missing body in when"))
      (analyze-children ctx body false))))

(defn analyze-clojure-string-replace [ctx expr]
  (let [children (next (:children expr))
        arg-types (:arg-types ctx)]
    (dorun (analyze-children ctx children false))
    (when arg-types
      (let [types @arg-types
            types (rest (map :tag types))
            match-type (types/keyword (first types))
            matcher-type (second types)
            matcher-type (types/keyword matcher-type)]
        (when (and match-type
                   matcher-type
                   (not (identical? matcher-type :any)))
          (case match-type
            :string (when (not (identical? matcher-type :string))
                      (findings/reg-finding!
                       ctx
                       (node->line (:filename ctx) (last children)
                                   :type-mismatch
                                   "String match arg requires string replacement arg.")))
            :char (when (not (identical? matcher-type :char))
                    (findings/reg-finding!
                     ctx
                     (node->line (:filename ctx) (last children)
                                 :type-mismatch
                                 "Char match arg requires char replacement arg.")))
            :regex (when (not (or (identical? matcher-type :string)
                                  (identical? matcher-type :nilable/string)
                                  ;; we could allow :ifn here, but keywords are
                                  ;; not valid in this position, so we do an
                                  ;; additional check for :map
                                  (identical? matcher-type :fn)
                                  (identical? matcher-type :map)))
                     (findings/reg-finding!
                      ctx
                      (node->line (:filename ctx) (last children)
                                  :type-mismatch
                                  "Regex match arg requires string or function replacement arg.")))
            nil))))))

(defn analyze-proxy-super [ctx expr]
  (let [bindings (:bindings ctx)]
    (when-let [this-binding (get bindings 'this)]
      (let [ns-name (-> ctx :ns :name)]
        (namespace/reg-used-binding! ctx
                                     ns-name
                                     this-binding
                                     nil))))
  (analyze-children ctx (nnext (:children expr)) false))

(defn analyze-amap [ctx expr]
  (let [[_ array idx-binding ret-binding body] (:children expr)
        ctx (ctx-with-bindings ctx
                               (into {} (map #(extract-bindings ctx % expr {})
                                             [idx-binding ret-binding])))]
    (analyze-children ctx [array body] false)))

(defn analyze-format-string [ctx format-str-node format-str args]
  (let [;; we aren't interested in %% or %n
        format-str (str/replace format-str #"%[%n]" "")
        percents (re-seq #"%[^\s%]+" format-str)
        [indexed unindexed]
        (reduce (fn [[indexed unindexed] percent]
                  (if-let [[_ pos] (re-find #"^%(\d+)\$" percent)]
                    [(max indexed (Integer/parseInt pos)) unindexed]
                    [indexed (cond-> unindexed (not= (.charAt ^String percent 1) \<) inc)]))
                [0 0] percents)
        percent-count (max indexed unindexed)
        arg-count (count args)]
    (when-not (= percent-count
                 arg-count)
      (findings/reg-finding! ctx
                             (node->line (:filename ctx) format-str-node :format
                                         (format "Format string expects %s arguments instead of %s."
                                                 percent-count arg-count))))))

(defn analyze-format [ctx expr]
  (let [children (next (:children expr))
        format-str-node (first children)
        format-str (utils/string-from-token format-str-node)]
    (when format-str
      (analyze-format-string ctx format-str-node format-str (rest children)))
    (analyze-children ctx children false)))

(defn analyze-formatted-logging [ctx expr]
  (let [children (next (:children expr))]
    (loop [attempt 0
           args (seq children)]
      (when-first [a args]
        (if-let [format-str (utils/string-from-token a)]
          (analyze-format-string ctx a format-str (rest args))
          (when (zero? attempt)
            ;; format string can be either the first or second argument
            (recur (inc attempt) (rest args))))))
    (analyze-children ctx children false)))

(defn analyze-hof [ctx expr resolved-as-name hof-ns-name hof-resolved-name]
  (let [children (next (:children expr))
        core-ns? (or (= 'clojure.core hof-ns-name)
                     (= 'cljs.core hof-ns-name))
        [prepending-n f-pos f-args-n] (cond (and core-ns?
                                                 (or (= 'update hof-resolved-name)
                                                     (= 'update-in hof-resolved-name)
                                                     (= 'send-via hof-resolved-name)))
                                            [2 2 3]
                                            (and core-ns?
                                                 (or (= 'swap! hof-resolved-name)
                                                     (= 'swap-vals! hof-resolved-name)
                                                     (= 'send hof-resolved-name)
                                                     (= 'send-off hof-resolved-name)))
                                            [1 1 2]
                                            :else
                                            [0 0 1])
        [prepending f f-args] [(take prepending-n children)
                               (nth children f-pos nil)
                               (drop f-args-n children)]
        ;; _ (prn :prepending prepending :f f :f-args f-args)
        _ (analyze-children ctx prepending false)
        fana (analyze-expression** ctx f)
        fsym (utils/symbol-from-token f)
        binding (get (:bindings ctx) fsym)
        arity (if binding
                (get (:arities ctx) fsym)
                (-> fana meta :arity))
        ns (:ns ctx)
        var? (and fsym (not binding))
        ns-name (:name ns)
        {resolved-namespace :ns
         resolved-name :name
         resolved-alias :alias
         unresolved? :unresolved?
         unresolved-ns :unresolved-ns
         clojure-excluded? :clojure-excluded?
         interop? :interop?
         resolved-core? :resolved-core?
         :as _m} (when var?
                   (resolve-name ctx true ns-name fsym nil))
        var? (and fsym (not binding))
        arg-count (cond (one-of resolved-as-name [map mapv mapcat])
                        (count f-args)
                        (one-of resolved-as-name [update update-in send send-off send-via swap! swap-vals!])
                        (inc (count f-args))
                        (one-of resolved-as-name [reduce map-indexed keep-indexed]) 2
                        :else 1)
        transducer-eligable? (one-of resolved-as-name [map filter remove mapcat map-indexed
                                                       keep keep-indexed])
        arg-count (if (and transducer-eligable?
                           (zero? arg-count)) ;; transducer
                    (if (and core-ns?
                             (or (= 'map hof-resolved-name)
                                 (= 'mapcat hof-resolved-name)))
                      nil 1)
                    arg-count)
        ctx (update ctx :callstack
                    (fn [cs]
                      (cons [resolved-namespace resolved-name]
                            cs)))]
    (cond var?
          (let [{:keys [:row :end-row :col :end-col]} (meta f)]
            (when (:analyze-var-usages? ctx)
              (namespace/reg-var-usage! ctx ns-name
                                        {:type (if arg-count :call :usage)
                                         :resolved-ns resolved-namespace
                                         :ns ns-name
                                         :name (with-meta
                                                 (or resolved-name fsym)
                                                 (meta fsym))
                                         :alias resolved-alias
                                         :unresolved? unresolved?
                                         :allow-forward-reference? (:in-comment ctx)
                                         :unresolved-ns unresolved-ns
                                         :clojure-excluded? clojure-excluded?
                                         :arity arg-count
                                         :row row
                                         :end-row end-row
                                         :col col
                                         :end-col end-col
                                         :base-lang (:base-lang ctx)
                                         :lang (:lang ctx)
                                         :filename (:filename ctx)
                                         ;; save some memory during dependencies
                                         :expr (when-not (:dependencies ctx) expr)
                                         :simple? (simple-symbol? fsym)
                                         :callstack (:callstack ctx)
                                         :config (:config ctx)
                                         :top-ns (:top-ns ctx)
                                         ;; :arg-types (:arg-types ctx)
                                         :interop? interop?
                                         :resolved-core? resolved-core?
                                         :in-def (:in-def ctx)
                                         :derived-location (:derived-location (meta expr))})))
          (and arity arg-count)
          (let [{:keys [:fixed-arities :varargs-min-arity]} arity
                config (:config ctx)
                callstack (:callstack ctx)]
            (when-not (config/skip? config :invalid-arity callstack)
              (let [filename (:filename ctx)]
                (when-not (linter-disabled? ctx :invalid-arity)
                  (when-not (arity-match? fixed-arities varargs-min-arity arg-count)
                    (let [fst-ana (first fana)
                          fn-name (or fsym (:name fst-ana))]
                      (findings/reg-finding!
                       ctx
                       (node->line filename f
                                   :invalid-arity
                                   (linters/arity-error nil fn-name arg-count fixed-arities varargs-min-arity))))))))))
    (when (and (not (utils/linter-disabled? ctx :reduce-without-init))
               (= 'reduce hof-resolved-name)
               (or (= 'clojure.core hof-ns-name)
                   (= 'clojure.cljs hof-ns-name))
               (= 2 (count children))
               (not (one-of [resolved-namespace resolved-name]
                            [[clojure.core +] [cljs.core +]
                             [clojure.core *] [cljs.core *]]))
               (not (config/reduce-without-init-excluded? (:config ctx)
                                                          (symbol (str resolved-namespace)
                                                                  (str resolved-name)))))
      (findings/reg-finding!
       ctx
       (node->line (:filename ctx) expr
                   :reduce-without-init
                   "Reduce called without explicit initial value.")))
    (concat fana
            (analyze-children ctx f-args false))))

(defn analyze-ns-unmap [ctx base-lang lang ns-name expr]
  (let [[ns-expr sym-expr :as children] (rest (:children expr))]
    (when (= '*ns* (:value ns-expr))
      (let [t (tag sym-expr)]
        (when (identical? :quote t)
          (let [sym (first (:children sym-expr))
                sym (:value sym)]
            (when (simple-symbol? sym)
              (let [nss (:namespaces ctx)
                    ;; ns (get-in @nss [base-lang lang ns-name])
                    ]
                (swap! nss update-in [base-lang lang ns-name]
                       (fn [ns]
                         (-> ns
                             (update :clojure-excluded (fnil conj #{}) sym)
                             (update :vars dissoc sym))))))))))
    (analyze-children ctx children)))

(defn analyze-gen-class [ctx _expr base-lang lang current-ns]
  ;; for now we just ignore the form to not cause false positives
  ;; we can add more sophisticated linting, e.g. causing -init to be used
  (swap! (:namespaces ctx) assoc-in [base-lang lang current-ns :gen-class] true)
  nil)

(defn analyze-extend-type-children
  "Used for analyzing children of extend-protocol, extend-type, reify and specify! "
  [ctx children defined-by defined-by->lint-as]
  (analyze-protocol-impls ctx defined-by defined-by->lint-as (-> ctx :ns :name) children))

(defn analyze-reify [ctx expr defined-by defined-by->lint-as]
  (let [children (next (:children expr))]
    (analyze-extend-type-children ctx children defined-by defined-by->lint-as)))

(defn analyze-extend-type [ctx expr defined-by defined-by->lint-as]
  (let [children (next (:children expr))
        ctx (if (identical? :cljs (:lang ctx))
              (update-in ctx [:config :linters :unresolved-symbol :exclude]
                         (fn [config]
                           (conj config
                                 'number 'function 'default 'object 'string 'bigint)))
              ctx)]
    (analyze-extend-type-children ctx children defined-by defined-by->lint-as)))

(defn analyze-specify! [ctx expr defined-by defined-by->lint-as]
  (let [children (next (:children expr))
        expr (first children)
        _ (analyze-expression** ctx expr)]
    (analyze-extend-type ctx {:children children} defined-by defined-by->lint-as)))

(defn analyze-cljs-exists? [ctx expr]
  (run! #(analyze-usages2
          (utils/ctx-with-linters-disabled ctx [:unresolved-symbol :unresolved-namespace])
          %)
        (next (:children expr))))

(defn- analyze-instance-invocation [ctx expr children]
  ;; see https://clojure.org/reference/java_interop#dot
  (findings/warn-reflection ctx expr)
  (let [[instance meth & args] children]
    (if instance (analyze-expression** ctx instance)
        ;; TODO, warning, instance is required
        nil)
    (when meth
      (if (and (identical? :list (utils/tag meth)) (not args))
        (let [[meth & children] (:children meth)]
          (analysis/reg-instance-invocation! ctx meth)
          (analyze-children ctx children))
        (analysis/reg-instance-invocation! ctx meth)))
    (when args
      (analyze-children ctx args))))

(def with-precision-bindings
  (zipmap '[CEILING, FLOOR, HALF_UP, HALF_DOWN, HALF_EVEN, UP, DOWN, UNNECESSARY]
          (repeat {})))

(defn- analyze-with-precision [ctx _expr children]
  (analyze-children (utils/ctx-with-bindings ctx
                                             with-precision-bindings)
                    children))

(defn- analyze-= [ctx expr]
  (let [[lhs rhs :as children] (rest (:children expr))
        ;; need to analyze children, to pick up on ignores in arguments
        res (analyze-children ctx children false)]
    (when (= 2 (count children))
      (when (or (true? (:value lhs))
                (true? (:value rhs)))
        (findings/reg-finding! ctx (assoc (meta expr)
                                          :type :equals-true
                                          :message "Prefer (true? x) over (= true x)"
                                          :filename (:filename ctx))))
      (let [cfg (-> ctx :config :linters :equals-expected-position)
            level (:level cfg)
            pos (-> cfg :position)
            only-in-test-assertion (-> cfg :only-in-test-assertion)]
        (when-let [expr (when-not (identical? :off level)
                          (or
                           (and (identical? :first pos)
                                (utils/constant? rhs)
                                (not (utils/constant? lhs))
                                (or (not only-in-test-assertion)
                                    (one-of (second (:callstack ctx)) [[cljs.test is] [clojure.test is]]))
                                rhs)
                           (and (identical? :last pos)
                                (utils/constant? lhs)
                                (not (utils/constant? rhs))
                                (or (not only-in-test-assertion)
                                    (one-of (second (:callstack ctx)) [[cljs.test is] [clojure.test is]]))
                                lhs)))]
          (findings/reg-finding! ctx (assoc (meta expr)
                                            :type :equals-expected-position
                                            :message (str "Write expected value " (name pos))
                                            :filename (:filename ctx))))
        (when (or (false? (:value lhs))
                  (false? (:value rhs)))
          (findings/reg-finding! ctx (assoc (meta expr)
                                            :type :equals-false
                                            :message "Prefer (false? x) over (= false x)"
                                            :filename (:filename ctx))))))
    res))

(defn- analyze-+- [ctx sym expr]
  (let [plus? (= '+ sym)
        minus? (= '- sym)
        [lhs rhs :as children] (rest (:children expr))]
    (when (and (= 2 (count children))
               (or (and plus? (= 1 (:value lhs)))
                   (= 1 (:value rhs))))
      (when (or plus? minus?)
        (findings/reg-finding! ctx (assoc (meta expr)
                                          :type (if plus? :plus-one :minus-one)
                                          :message (if plus?
                                                     "Prefer (inc x) over (+ 1 x)"
                                                     "Prefer (dec x) over (- x 1)")
                                          :filename (:filename ctx)))))
    (analyze-children ctx children false)))

(defn- analyze-var [ctx expr children]
  (when (:condition expr)
    (findings/reg-finding! ctx (assoc (meta expr) :filename (:filename ctx) :message "Condition always true" :type :condition-always-true)))
  (analyze-children (assoc ctx :private-access? true) children))

(defn analyze-call
  [{:keys [:top-level? :base-lang :lang :ns :config :dependencies] :as ctx}
   {:keys [:arg-count
           :full-fn-name
           :row :col
           :expr] :as m}]
  (let [not-is-dot (and (not= '. full-fn-name)
                        (not= '.. full-fn-name))]
    (cond
      (and not-is-dot
           (str/ends-with? full-fn-name "."))
      (recur ctx
             (let [expr (macroexpand/expand-dot-constructor ctx expr)]
               (assoc m
                      :expr expr
                      :full-fn-name 'new
                      :arg-count (inc (:arg-count m)))))
      (and not-is-dot
           (str/starts-with? full-fn-name "."))
      (recur ctx
             (let [expr (macroexpand/expand-method-invocation ctx expr)]
               (assoc m
                      :expr expr
                      :full-fn-name '.
                      :arg-count (inc (:arg-count m)))))
      :else
      (let [ns-name (:name ns)
            children (:children expr)
            name-node (first children)
            children (rest children)
            {resolved-namespace :ns
             resolved-name :name
             resolved-alias :alias
             unresolved? :unresolved?
             allow-forward-reference? :allow-forward-reference?
             unresolved-ns :unresolved-ns
             clojure-excluded? :clojure-excluded?
             interop? :interop?
             resolved-core? :resolved-core?
             :as _m}
            (resolve-name ctx true ns-name full-fn-name expr)
            expr-meta (meta expr)
            cfg (when-let [in-call-cfg (:config-in-call config)]
                  (get in-call-cfg (symbol (str resolved-namespace) (str resolved-name))))
            cfg (when cfg
                  (config/expand-ignore cfg))
            ctx (if cfg
                  (update ctx :config config/merge-config! cfg)
                  ctx)
            prev-callstack (:callstack ctx)
            arg-types (if (and resolved-namespace resolved-name
                               (not (linter-disabled? ctx :type-mismatch)))
                        (atom [])
                        nil)
            ctx (assoc ctx :arg-types arg-types)]
        (cond unresolved-ns
              (do
                (namespace/reg-unresolved-namespace! ctx ns-name
                                                     (with-meta unresolved-ns
                                                       (meta full-fn-name)))
                (analyze-children (update ctx :callstack conj [:clj-kondo/unknown-namespace
                                                               (symbol (name full-fn-name))])
                                  children))
              :else
              (let [[resolved-as-namespace resolved-as-name _lint-as?]
                    (or (when-let
                         [[ns n]
                          (config/lint-as config
                                          [resolved-namespace resolved-name])]
                          [ns n true])
                        [resolved-namespace resolved-name false])
                    ;; See #1170, we deliberaly use resolved and not resolved-as
                    ;; Users can get :lint-as like behavior for hooks by configuring
                    ;; multiple fns to target the same hook code
                    hook-fn
                    (let [visited (:visited expr)]
                      (when-not (and visited (= visited [resolved-namespace resolved-name]))
                        (or
                         (hooks/hook-fn ctx config resolved-namespace resolved-name)
                         (case [resolved-namespace resolved-name]
                           ([clojure.test testing] [cljs.test testing])
                           (when (:analysis-context ctx)
                                ;; only use testing hook when analysis is requested
                             test/testing-hook)
                           nil))))
                    transformed (when hook-fn
                              ;;;; Expand macro using user-provided function
                                  (let [filename (:filename ctx)]
                                    (binding [utils/*ctx* ctx]
                                      (sci/binding [sci/out *out*
                                                    sci/err *err*]
                                        (try (hook-fn {:node expr
                                                       :cljc (identical? :cljc base-lang)
                                                       :lang lang
                                                       :filename filename
                                                       :config config
                                                       :ns ns-name
                                                       :context (:context ctx)})
                                             (catch Throwable e
                                               (findings/reg-finding!
                                                ctx
                                                (merge
                                                 {:filename filename
                                                  :row row
                                                  :col col
                                                  :type :hook
                                                  :message (.getMessage e)}
                                                 (select-keys (ex-data e)
                                                              [:level :row :col])))
                                               nil))))))
                    ctx (if-let [context (when transformed
                                           (:context transformed))]
                          (assoc ctx :context context)
                          ctx)]
                (if-let [expanded (and transformed
                                       (let [node (:node transformed)]
                                         (when-not (identical? expr node)
                                           node)))]
                  (let [expanded (hooks/annotate expanded expr-meta)
                        [new-name-node new-arg-count]
                        (when (utils/list-node? expanded)
                          (when-let [children (:children expanded)]
                            [(first children)
                             (dec (count children))]))
                        same-call? (and new-name-node
                                        new-arg-count
                                        (= (utils/tag name-node)
                                           (utils/tag new-name-node))
                                        (= full-fn-name (:value new-name-node))
                                        (= arg-count
                                           new-arg-count))
                        expanded (assoc expanded :visited [resolved-namespace resolved-name])]
                    ;;;; This registers the original call when the new node does not
                    ;;;; refer to the same call, so we still get arity linting
                    (when (and (:analyze-var-usages? ctx)
                               (not same-call?))
                      (namespace/reg-var-usage!
                       ctx ns-name {:type :call
                                    :resolved-ns resolved-namespace
                                    :ns ns-name
                                    :name (with-meta
                                            (or resolved-name full-fn-name)
                                            (meta full-fn-name))
                                    :alias resolved-alias
                                    :unresolved? unresolved?
                                    :unresolved-ns unresolved-ns
                                    :clojure-excluded? clojure-excluded?
                                    :arity arg-count
                                    :row row
                                    :end-row (:end-row expr-meta)
                                    :col col
                                    :end-col (:end-col expr-meta)
                                    :base-lang base-lang
                                    :lang lang
                                    :filename (:filename ctx)
                                    ;; save some memory during dependencies
                                    :expr (when-not dependencies expr)
                                    :simple? (simple-symbol? full-fn-name)
                                    :callstack (:callstack ctx)
                                    :config (:config ctx)
                                    :top-ns (:top-ns ctx)
                                    :arg-types arg-types
                                    :interop? interop?
                                    :resolved-core? resolved-core?
                                    :idx (:idx ctx)
                                    :len (:len ctx)
                                    :derived-location (:derived-location expr-meta)}))
                    ;;;; This registers the namespace as used, to prevent unused warnings
                    (namespace/reg-used-namespace! ctx
                                                   ns-name
                                                   resolved-namespace)
                    (let [node expanded]
                      (analyze-expression** (assoc-some ctx :defined-by (:defined-by transformed))
                                            node)))
                  ;;;; End macroexpansion
                  (let [fq-sym (when (and resolved-namespace
                                          resolved-name)
                                 (symbol (str resolved-namespace)
                                         (str resolved-name)))
                        unknown-ns? (= :clj-kondo/unknown-namespace resolved-namespace)
                        resolved-namespace* (if unknown-ns?
                                              ns-name resolved-namespace)
                        ctx (if (and fq-sym
                                     (not (one-of fq-sym [clojure.core/doto])))
                              (update ctx :callstack
                                      (fn [cs]
                                        (let [generated? (:clj-kondo.impl/generated expr)]
                                          (cons (with-meta [resolved-namespace* resolved-name]
                                                  (cond-> expr-meta
                                                    generated?
                                                    (assoc :clj-kondo.impl/generated true))) cs))))
                              (update ctx :callstack conj [nil nil]))
                        resolved-as-clojure-var-name
                        (when (one-of resolved-as-namespace [clojure.core cljs.core])
                          resolved-as-name)
                        ctx (if resolved-as-clojure-var-name
                              (assoc ctx
                                     :resolved-as-clojure-var-name resolved-as-clojure-var-name)
                              ctx)
                        defined-by (or (:defined-by ctx)
                                       (when (and resolved-name resolved-namespace)
                                         (symbol (name resolved-namespace)
                                                 (name resolved-name))))
                        defined-by->lint-as (or (:defined-by->lint-as ctx)
                                                (when (and resolved-as-name resolved-as-namespace)
                                                  (symbol (name resolved-as-namespace)
                                                          (name resolved-as-name))))
                        analyzed
                        (case resolved-as-clojure-var-name
                          ns
                          (when top-level?
                            [(analyze-ns-decl ctx expr)])
                          in-ns (if top-level? [(analyze-in-ns ctx expr)]
                                    (analyze-children ctx children))
                          alias
                          [(analyze-alias ctx expr)]
                          declare (analyze-declare ctx expr defined-by defined-by->lint-as)
                          (def defonce defmulti goog-define)
                          (do (lint-inline-def! ctx expr)
                              (analyze-def ctx expr defined-by defined-by->lint-as))
                          (defn defn- defmacro definline)
                          (do (lint-inline-def! ctx expr)
                              (analyze-defn ctx expr defined-by defined-by->lint-as))
                          defmethod (analyze-defmethod ctx expr)
                          (definterface defprotocol) (analyze-defprotocol ctx expr defined-by defined-by->lint-as)
                          (defrecord deftype) (analyze-defrecord ctx expr defined-by defined-by->lint-as)
                          comment
                          (let [cfg (:config-in-comment config)
                                ctx (if cfg
                                      (assoc ctx :config (config/merge-config! config cfg))
                                      ctx)
                                ctx (assoc ctx :in-comment true)]
                            (analyze-children ctx children))
                          (-> some->)
                          (analyze-expression** ctx (macroexpand/expand-> ctx expr))
                          (->> some->>)
                          (analyze-expression** ctx (macroexpand/expand->> ctx expr))
                          doto
                          (analyze-expression** ctx (macroexpand/expand-doto ctx expr))
                          reify (analyze-reify ctx expr defined-by defined-by->lint-as)
                          (extend-protocol extend-type) (analyze-extend-type ctx expr defined-by defined-by->lint-as)
                          (specify!) (analyze-specify! ctx expr defined-by defined-by->lint-as)

                          (.) (analyze-instance-invocation ctx expr children)
                          (..) (analyze-expression** ctx (macroexpand/expand-double-dot ctx expr))
                          (proxy defcurried)
                          ;; don't lint calls in these expressions, only register them as used vars
                          (analyze-children (utils/ctx-with-linters-disabled ctx [:invalid-arity
                                                                                  :unresolved-symbol
                                                                                  :type-mismatch])
                                            children)
                          (proxy-super)
                          (analyze-proxy-super ctx expr)
                          (amap)
                          (analyze-amap ctx expr)
                          (cond-> cond->>)
                          (analyze-expression** ctx (macroexpand/expand-cond->
                                                     ctx expr
                                                     resolved-as-name))
                          (let let* for doseq dotimes with-open with-local-vars)
                          (analyze-like-let ctx expr)
                          letfn
                          (analyze-letfn ctx expr)
                          (if-let if-some when-let when-some when-first)
                          (analyze-conditional-let ctx resolved-as-clojure-var-name expr)
                          do
                          (analyze-do ctx expr)
                          (fn fn* bound-fn)
                          (analyze-fn ctx expr)
                          case
                          (analyze-case ctx expr)
                          loop
                          (analyze-loop ctx expr)
                          recur
                          (analyze-recur ctx expr)
                          quote nil
                          try (analyze-try ctx expr)
                          as-> (analyze-as-> ctx expr)
                          areduce (analyze-areduce ctx expr)
                          this-as (analyze-this-as ctx expr)
                          memfn (analyze-memfn ctx expr)
                          empty? (analyze-empty? ctx expr)
                          format (analyze-format ctx expr)
                          (use require)
                          (if top-level? (namespace-analyzer/analyze-require ctx expr)
                              (analyze-children ctx children))
                          import
                          (if top-level? (analyze-import ctx expr)
                              (analyze-children ctx children))
                          if (analyze-if ctx expr)
                          new (analyze-constructor ctx expr)
                          set! (analyze-set! ctx expr)
                          = (analyze-= ctx expr)
                          (+ -) (analyze-+- ctx resolved-name expr)
                          (with-redefs binding) (analyze-with-redefs ctx expr)
                          (when when-not) (analyze-when ctx expr)
                          (map mapv filter filterv remove reduce
                               every? not-every? some not-any? mapcat iterate
                               max-key min-key group-by partition-by map-indexed
                               keep keep-indexed update update-in swap! swap-vals!
                               send send-off send-via)
                          (analyze-hof ctx expr resolved-as-name resolved-namespace resolved-name)
                          (ns-unmap) (analyze-ns-unmap ctx base-lang lang ns-name expr)
                          (gen-class) (analyze-gen-class ctx expr base-lang lang ns-name)
                          (exists?) (analyze-cljs-exists? ctx expr)
                          (with-precision) (analyze-with-precision ctx expr children)
                          (var) (analyze-var ctx expr children)
                          ;; catch-all
                          (case [resolved-as-namespace resolved-as-name]
                            [clj-kondo.lint-as def-catch-all]
                            (analyze-def-catch-all ctx expr)
                            [schema.core fn]
                            (analyze-schema ctx 'fn expr 'schema.core/fn defined-by->lint-as)
                            [schema.core def]
                            (analyze-schema ctx 'def expr 'schema.core/def defined-by->lint-as)
                            [schema.core defn]
                            (analyze-schema ctx 'defn expr 'schema.core/defn defined-by->lint-as)
                            [schema.core defmethod]
                            (analyze-schema ctx 'defmethod expr 'schema.core/defmethod defined-by->lint-as)
                            [schema.core defrecord]
                            (analyze-schema ctx 'defrecord expr 'schema.core/defrecord defined-by->lint-as)
                            ([clojure.test deftest]
                             [clojure.test deftest-]
                             [cljs.test deftest])
                            (test/analyze-deftest ctx expr defined-by defined-by->lint-as)
                            ([clojure.core.match match] [cljs.core.match match])
                            (match/analyze-match ctx expr)
                            [clojure.string replace]
                            (analyze-clojure-string-replace ctx expr)
                            [cljs.test async]
                            (test/analyze-cljs-test-async ctx expr)
                            ([clojure.test are] [cljs.test are])
                            (test/analyze-are ctx resolved-namespace expr)
                            ([clojure.test.check.properties for-all])
                            (analyze-like-let ctx expr)
                            [cljs.spec.alpha def]
                            (spec/analyze-def ctx expr 'cljs.spec.alpha/def)
                            [clojure.spec.alpha def]
                            (spec/analyze-def ctx expr 'clojure.spec.alpha/def)
                            ([clojure.spec.alpha fdef] [cljs.spec.alpha fdef])
                            (spec/analyze-fdef (assoc ctx
                                                      :analyze-children
                                                      analyze-children) expr)
                            ([clojure.spec.alpha keys] [cljs.spec.alpha keys])
                            (spec/analyze-keys ctx expr)
                            ([clojure.spec.gen.alpha lazy-combinators]
                             [clojure.spec.gen.alpha lazy-prims])
                            (analyze-declare ctx expr defined-by defined-by->lint-as)
                            [potemkin import-vars]
                            (potemkin/analyze-import-vars ctx expr utils/ctx-with-linters-disabled
                                                          'potemkin/import-vars
                                                          defined-by->lint-as)
                            ([clojure.core.async alt!] [clojure.core.async alt!!]
                                                       [cljs.core.async alt!] [cljs.core.async alt!!])
                            (core-async/analyze-alt!
                             (assoc ctx
                                    :analyze-expression** analyze-expression**
                                    :extract-bindings extract-bindings)
                             expr)
                            ([clojure.core.async defblockingop])
                            (analyze-defn ctx expr defined-by defined-by->lint-as)
                            ([clojure.core.reducers defcurried])
                            (analyze-defn ctx expr defined-by defined-by->lint-as)
                            ([clojure.template do-template])
                            (analyze-expression** ctx (macroexpand/expand-do-template ctx expr))
                            ([datahike.api q]
                             [datascript.core q]
                             [datomic.api q]
                             [datomic.client.api q]
                             [datalevin.core q]
                             [datomic-type-extensions.api q])
                            (do (datalog/analyze-datalog ctx expr)
                                (analyze-children ctx children false))
                            ([compojure.core GET]
                             [compojure.core POST]
                             [compojure.core PUT]
                             [compojure.core DELETE]
                             [compojure.core HEAD]
                             [compojure.core OPTIONS]
                             [compojure.core PATCH]
                             [compojure.core ANY]
                             [compojure.core context]
                             [compojure.core rfn])
                            (compojure/analyze-compojure-macro ctx expr resolved-as-name)
                            ([clojure.java.jdbc with-db-transaction]
                             [clojure.java.jdbc with-db-connection]
                             [clojure.java.jdbc with-db-metadata]
                             [next.jdbc with-transaction])
                            (jdbc/analyze-like-jdbc-with ctx expr)
                            ([clojure.tools.logging debugf]
                             [clojure.tools.logging infof]
                             [clojure.tools.logging errorf]
                             [clojure.tools.logging logf]
                             [clojure.tools.logging spyf]
                             [clojure.tools.logging tracef]
                             [clojure.tools.logging warnf])
                            (analyze-formatted-logging ctx expr)
                            [clojure.data.xml alias-uri]
                            (xml/analyze-alias-uri ctx expr)
                            [clojure.data.xml.impl export-api]
                            (xml/analyze-export-api ctx expr)
                            [cljs.core simple-benchmark]
                            (analyze-like-let ctx expr)
                            [babashka.process $]
                            (babashka/analyze-$ ctx expr)
                            ([re-frame.core reg-event-db]
                             [re-frame.core reg-event-ctx]
                             [re-frame.core reg-sub-raw]
                             [re-frame.core reg-fx]
                             [re-frame.core reg-cofx])
                            (re-frame/analyze-reg
                             ctx expr
                             (symbol (str resolved-namespace) (str resolved-name)))
                            ([re-frame.core subscribe])
                            (re-frame/analyze-subscribe ctx expr (str resolved-namespace))
                            ([re-frame.core dispatch]
                             [re-frame.core dispatch-sync])
                            (re-frame/analyze-dispatch ctx expr (str resolved-namespace))
                            ([re-frame.core reg-sub])
                            (re-frame/analyze-reg-sub ctx expr (symbol (str resolved-namespace)
                                                                       (str resolved-name)))
                            ([re-frame.core reg-event-fx])
                            (re-frame/analyze-reg-event-fx ctx expr (symbol (str resolved-namespace)
                                                                            (str resolved-name)))
                            ([re-frame.core inject-cofx])
                            (re-frame/analyze-inject-cofx ctx expr (str resolved-namespace))
                            ;; catch-all
                            (let [next-ctx (cond-> ctx
                                             (one-of [resolved-namespace resolved-name]
                                                     [[clojure.core.async thread]
                                                      [clojure.core dosync]
                                                      [clojure.core future]
                                                      [clojure.core lazy-seq]
                                                      [clojure.core lazy-cat]])
                                             (-> (assoc-in [:recur-arity :fixed-arity] 0)
                                                 (assoc :seen-recur? (volatile! nil))
                                                 (dissoc :protocol-fn)))]
                              (analyze-children next-ctx children false))))]
                    (if (= 'ns resolved-as-clojure-var-name)
                      analyzed
                      (let [in-def (:in-def ctx)
                            id (:id expr)
                            m (meta analyzed)
                            context (when (:analysis-context ctx)
                                      (let [node-context (:context name-node)
                                            ctx-context (:context ctx)
                                            context (utils/deep-merge
                                                     ctx-context
                                                     node-context)]
                                        context))
                            fn-parent-loc (redundant-fn-wrapper ctx prev-callstack children interop?)
                            proto-call {:type :call
                                        :context context
                                        :resolved-ns resolved-namespace
                                        :ns ns-name
                                        :name (with-meta
                                                (or resolved-name full-fn-name)
                                                (meta full-fn-name))
                                        :alias resolved-alias
                                        :unresolved? unresolved?
                                        :allow-forward-reference? allow-forward-reference?
                                        :unresolved-ns unresolved-ns
                                        :clojure-excluded? clojure-excluded?
                                        :arity arg-count
                                        :row row
                                        :end-row (:end-row expr-meta)
                                        :col col
                                        :end-col (:end-col expr-meta)
                                        :base-lang base-lang
                                        :lang lang
                                        :filename (:filename ctx)
                                        :expr (when-not dependencies expr)
                                        :callstack (:callstack ctx)
                                        :config (:config ctx)
                                        :top-ns (:top-ns ctx)
                                        :arg-types (:arg-types ctx)
                                        :simple? (simple-symbol? full-fn-name)
                                        :interop? interop?
                                        :resolved-core? resolved-core?
                                        :redundant-fn-wrapper-parent-loc fn-parent-loc
                                        :idx (:idx ctx)
                                        :len (:len ctx)
                                        :derived-location (:derived-location expr-meta)}
                            ret-tag (or (:ret m)
                                        (types/ret-tag-from-call ctx proto-call expr))
                            call (cond-> proto-call
                                   id (assoc :id id)
                                   in-def (assoc :in-def in-def)
                                   ret-tag (assoc :ret ret-tag))]
                        (utils/reg-call ctx call id)
                        (when (:analyze-var-usages? ctx)
                          (namespace/reg-var-usage! ctx ns-name call))
                        (when (not unresolved?)
                          (namespace/reg-used-namespace! ctx
                                                         ns-name
                                                         resolved-namespace))
                        (if m
                          (with-meta (cons call analyzed)
                            m)
                          (cons call analyzed))))))))))))

(defn analyze-keyword-call
  [{:keys [:base-lang :lang :ns] :as ctx}
   {:keys [:arg-count
           :full-fn-name
           :row :col
           :expr]}]
  (let [ns-name (:name ns)
        children (:children expr)
        kw-node (first children)
        _ (usages/analyze-keyword ctx kw-node)
        children (rest children)
        expr-meta (meta expr)
        resolved-namespace :clj-kondo/unknown-namespace
        ctx (update ctx :callstack conj [nil :token])
        arg-types (if (not (linter-disabled? ctx :type-mismatch))
                    (atom [])
                    nil)
        ctx (assoc ctx :arg-types arg-types)
        analyzed
        (let [next-ctx ctx]
          (analyze-children next-ctx children false))
        in-def (:in-def ctx)
        id (:id expr)
        m (meta analyzed)
        proto-call {:type :call
                    :resolved-ns resolved-namespace
                    :ns ns-name
                    :name full-fn-name
                    :unresolved? true
                    :unresolved-ns nil
                    :arity arg-count
                    :row row
                    :end-row (:end-row expr-meta)
                    :col col
                    :end-col (:end-col expr-meta)
                    :base-lang base-lang
                    :lang lang
                    :filename (:filename ctx)
                    :arg-types (:arg-types ctx)}
        ret-tag (or (:ret m)
                    (types/ret-tag-from-call ctx proto-call expr))
        call (cond-> proto-call
               id (assoc :id id)
               in-def (assoc :in-def in-def)
               ret-tag (assoc :ret ret-tag))]
    (utils/reg-call ctx call id)
    (if m
      (with-meta (cons call analyzed)
        m)
      (cons call analyzed))))

;; pulled out from lint-keyword-call!
(defn- resolve-keyword
  [ctx kw namespaced?]
  (let [ns (:ns ctx)
        ?resolved-ns (if namespaced?
                       (if-let [kw-ns (namespace kw)]
                         (or (get (:qualify-ns ns) (symbol kw-ns))
                             ;; because we couldn't resolve the namespaced
                             ;; keyword, we print it as is
                             (str ":" (namespace kw)))
                         ;; if the keyword is namespace, but there is no
                         ;; namespace, it's the current ns
                         (:name ns))
                       (namespace kw))]
    (if ?resolved-ns
      (keyword (str ?resolved-ns) (name kw))
      kw)))

(defn lint-keyword-call! [ctx kw namespaced? arg-count expr]
  (let [callstack (:callstack ctx)
        config (:config ctx)]
    (when-not (config/skip? config :invalid-arity callstack)
      (let [resolved-kw (resolve-keyword ctx kw namespaced?)
            kw-str (if (namespace resolved-kw)
                     (str (namespace resolved-kw) "/" (name kw))
                     (str (name resolved-kw)))]
        (when (or (zero? arg-count)
                  (> arg-count 2))
          (findings/reg-finding! ctx
                                 (node->line (:filename ctx) expr :invalid-arity
                                             (format "keyword :%s is called with %s args but expects 1 or 2"
                                                     kw-str
                                                     arg-count))))))
    (when-let [fn-parent-loc (redundant-fn-wrapper ctx callstack (rest (:children expr)) false)]
      (findings/reg-finding!
       ctx
       (assoc fn-parent-loc
              :filename (:filename ctx)
              :type :redundant-fn-wrapper
              :message "Redundant fn wrapper")))))

(defn lint-map-call! [ctx _the-map arg-count expr]
  (let [callstack (:callstack ctx)
        config (:config ctx)]
    (when (or (zero? arg-count)
              (> arg-count 2))
      (when-not (config/skip? config :invalid-arity callstack)
        (findings/reg-finding!
         ctx
         (node->line (:filename ctx) expr :invalid-arity
                     (format "map is called with %s args but expects 1 or 2"
                             arg-count)))))))

(defn lint-vector-call! [ctx _the-map arg-count expr]
  (let [callstack (:callstack ctx)
        config (:config ctx)]
    (when (not= 1 arg-count)
      (when-not (config/skip? config :invalid-arity callstack)
        (findings/reg-finding!
         ctx
         (node->line (:filename ctx) expr :invalid-arity
                     (str "Vector can only be called with 1 arg but was called with: "
                          arg-count)))))))

(defn lint-symbol-call! [ctx _the-symbol arg-count expr]
  (let [callstack (:callstack ctx)
        config (:config ctx)]
    (when (or (zero? arg-count)
              (> arg-count 2))
      (when-not (config/skip? config :invalid-arity callstack)
        (findings/reg-finding!
         ctx
         (node->line (:filename ctx) expr :invalid-arity
                     (format "symbol is called with %s args but expects 1 or 2"
                             arg-count)))))))

;; TODO: this should just be a case of :type-mismatch
(defn reg-not-a-function! [ctx expr typ]
  (let [callstack (:callstack ctx)
        config (:config ctx)]
    (when-not (config/skip? config :not-a-function callstack)
      (findings/reg-finding!
       ctx
       (node->line (:filename ctx) expr :not-a-function (str "a " typ " is not a function"))))))

(def default-cfg-in-tag {:linters {:unresolved-symbol {:level :off}
                                   :invalid-arity {:level :off}}})

(defn lint-discouraged-tags!
  [ctx tag-expr]
  (let [discouraged-tag-config (get-in ctx [:config :linters :discouraged-tag])
        tag (:value tag-expr)]
    (when-not (or (identical? :off (:level discouraged-tag-config))
                  (empty? (dissoc discouraged-tag-config :level)))
      (when-let [cfg (get discouraged-tag-config tag)]
        (findings/reg-finding! ctx
                               (assoc-some (node->line (:filename ctx)
                                                       tag-expr
                                                       :discouraged-tag
                                                       (or (:message cfg)
                                                           (str "Discouraged tag literal: " tag)))
                                           :level
                                           (:level cfg)))))))

(defn analyze-reader-macro [ctx expr]
  (let [children (:children expr)
        tag-expr (first children)
        tag (:value tag-expr)
        ctx (if (and (identical? :cljs (:lang ctx))
                     (= 'js tag))
              ctx
              (if-let [config (get-in ctx [:config :config-in-tag tag])]
                (update ctx :config utils/deep-merge default-cfg-in-tag config)
                (utils/ctx-with-linters-disabled ctx [:unresolved-symbol
                                                      :invalid-arity
                                                      :not-a-function])))
        children (rest children)]
    (lint-discouraged-tags! ctx tag-expr)
    (analyze-children ctx children)))

(defn- lint-unused-value [ctx expr]
  (let [idx (:idx ctx)
        len (:len ctx)
        callstack (:callstack ctx)]
    (when (and (symbol? (ffirst callstack))
               idx len (< idx (dec len)))
      (let [parent-call (first (:callstack ctx))
            core? (one-of (first parent-call) [clojure.core cljs.core])
            core-sym (when core?
                       (second parent-call))
            generated? (:clj-kondo.impl/generated expr)
            redundant?
            (and (not generated?)
                 core?
                 (not (:clj-kondo.impl/generated (meta parent-call)))
                 (one-of core-sym [do fn defn defn-
                                   let when-let loop binding with-open
                                   doseq try when when-not when-first
                                   when-some future]))]
        (when redundant?
          (findings/reg-finding! ctx (assoc (meta expr)
                                            :type :unused-value
                                            :message "Unused value"
                                            :filename (:filename ctx))))))))

#_(requiring-resolve 'clojure.set/union)

(defn analyze-expression**
  [{:keys [bindings lang] :as ctx}
   {:keys [children] :as expr}]
  (when expr
    (let [expr (if (or (not= :edn lang)
                       (:quoted ctx))
                 (meta/lift-meta-content2 (dissoc ctx :arg-types) expr)
                 expr)
          t (tag expr)
          {:keys [row col]} (meta expr)
          arg-count (count (rest children))]
      (utils/handle-ignore ctx expr)
      ;; map's type is added in :map handler below
      ;; namespaced map's type is added when going through analyze-expression** via analyze-namespaced-map
      ;; list and quote are handled specially because of return types
      ;; deref is handled via expansion
      (when-not (one-of t [:namespaced-map :map :list :quote :token :deref])
        ;; TODO: add types for all token cases!
        (types/add-arg-type-from-expr ctx expr))
      (case t
        :quote (do
                 (lint-unused-value ctx expr)
                 (let [ctx (assoc ctx :quoted true)]
                   (types/add-arg-type-from-expr ctx (first (:children expr)))
                   (analyze-children ctx children)))
        :syntax-quote (do
                        (lint-unused-value ctx expr)
                        (analyze-usages2 (assoc ctx :arg-types nil) expr))
        :var (do
               (lint-unused-value ctx expr)
               (analyze-var ctx expr (:children expr)))
        :reader-macro (do
                        (when (and (not (identical? :cljc (:base-lang ctx)))
                                   (str/starts-with? (-> expr :children first str) "?"))
                          (findings/reg-finding! ctx (assoc (meta expr)
                                                            :filename (:filename ctx)
                                                            :level :error
                                                            :type :syntax
                                                            :message "Reader conditionals are only allowed in .cljc files")))
                        (analyze-reader-macro ctx expr))
        (:unquote :unquote-splicing)
        (analyze-children ctx children)
        :namespaced-map (do
                          (lint-unused-value ctx expr)
                          (usages/analyze-namespaced-map
                           (-> ctx
                               (assoc :analyze-expression**
                                      analyze-expression**)
                               (update :callstack #(cons [nil t] %)))
                           expr))
        :map (do
               (lint-unused-value ctx expr)
               (key-linter/lint-map-keys ctx expr)
               (let [children (if (:data-readers ctx)
                                (map (fn [child k]
                                       (assoc child :clj-kondo.internal/map-position k))
                                     children
                                     (cycle [:key :val]))
                                children)
                     children (map (fn [c s]
                                     (assoc c :id s))
                                   children
                                   (repeatedly gensym))
                     analyzed (analyze-children
                               (update ctx
                                       :callstack #(cons [nil t] %)) children)]
                 (types/add-arg-type-from-expr ctx (assoc expr
                                                          :children children
                                                          :analyzed analyzed))
                 analyzed))
        :set (do (lint-unused-value ctx expr)
                 (key-linter/lint-set ctx expr)
                 (analyze-children (update ctx
                                           :callstack #(cons [nil t] %))
                                   children))
        :fn (do
              (lint-unused-value ctx expr)
              (when (and (:in-fn-literal ctx)
                         (not (:clj-kondo.impl/generated expr)))
                (findings/reg-finding! ctx (assoc (meta expr)
                                                  :filename (:filename ctx)
                                                  :level :error
                                                  :type :syntax
                                                  :message "Nested #()s are not allowed")))
              (when (identical? :edn lang)
                (findings/reg-finding! ctx (assoc (meta expr)
                                                  :filename (:filename ctx)
                                                  :level :error
                                                  :type :syntax
                                                  :message "#()s are not allowed in EDN")))
              (let [expanded-node (macroexpand/expand-fn expr)
                    m (meta expanded-node)
                    has-first-arg? (:clj-kondo.impl/fn-has-first-arg m)]
                (recur (cond-> (assoc ctx :arg-types nil :in-fn-literal true)
                         has-first-arg? (update :bindings assoc '% {}))
                       expanded-node)))
        :token
        (let [edn? (= :edn lang)]
          (if (or edn?
                  (:quoted ctx))
            (if (:k expr)
              (do (usages/analyze-keyword ctx expr)
                  (types/add-arg-type-from-expr ctx expr))
              (when-let [sym (utils/symbol-from-token expr)]
                (when (and (:analyze-symbols? ctx)
                           (qualified-symbol? sym))
                  (let [resolved-extra (or (when-not edn?
                                             (let [the-ns-name (-> ctx :ns :name)
                                                   resolved (namespace/resolve-name ctx false the-ns-name sym expr)]
                                               (when-not (or (:unresolved? resolved)
                                                             (:interop? resolved))
                                                 {:to (:ns resolved)
                                                  :name (:name resolved)})))
                                           {:name (symbol (name sym))})]
                    (analysis/reg-symbol!
                     ctx
                     (:filename ctx) (-> ctx :ns :name)
                     sym
                     lang (merge (meta expr) resolved-extra))))))
            (let [id (gensym)
                  expr (assoc expr :id id)
                  _ (analyze-usages2 ctx expr)
                  usage (get @(:calls-by-id ctx) id)]
              (if usage
                (types/add-arg-type-from-usage ctx usage expr)
                (types/add-arg-type-from-expr ctx expr))
              nil)))
        :list
        (if-let [function (some->>
                           (first children)
                           (meta/lift-meta-content2 (dissoc ctx :arg-types)))]
          (if (or (:quoted ctx) (= :edn lang))
            (analyze-children (update ctx :callstack (fn [cs]
                                                       (cons [:list nil] cs))) children)
            (let [t (tag function)]
              (case t
                :map
                (do (lint-map-call! ctx function arg-count expr)
                    (types/add-arg-type-from-expr ctx expr)
                    (analyze-children (update ctx :callstack conj [nil t]) children))
                :quote
                (let [quoted-child (-> function :children first)]
                  (types/add-arg-type-from-expr ctx expr)
                  (if (utils/symbol-token? quoted-child)
                    (do (lint-symbol-call! ctx quoted-child arg-count expr)
                        (analyze-children (update ctx :callstack conj [nil t])
                                          children))
                    (analyze-children (update ctx :callstack conj [nil t])
                                      children)))
                :vector
                (do (lint-vector-call! ctx function arg-count expr)
                    (types/add-arg-type-from-expr ctx expr)
                    (analyze-children (update ctx :callstack conj [nil t]) children))
                :token
                (if-let [k (:k function)]
                  (do (lint-keyword-call! ctx k (:namespaced? function) arg-count expr)
                      (let [[id expr] (if-let [id (:id expr)]
                                        [id expr]
                                        (let [id (gensym)]
                                          [id (assoc expr :id id)]))
                            ret (analyze-keyword-call ctx {:arg-count arg-count
                                                           :full-fn-name (resolve-keyword ctx k (:namespaced? function))
                                                           :row row
                                                           :col col
                                                           :expr expr})
                            maybe-call (some #(when (= id (:id %))
                                                %) ret)]
                        (if maybe-call
                          (types/add-arg-type-from-call ctx maybe-call expr)
                          (types/add-arg-type-from-expr ctx expr))
                        ret))
                  (if-let [full-fn-name (utils/symbol-from-token function)]
                    (let [simple? (simple-symbol? full-fn-name)
                          full-fn-name (if simple?
                                         (namespace/normalize-sym-name ctx full-fn-name)
                                         full-fn-name)
                          full-fn-name (with-meta full-fn-name (meta function))
                          binding (and simple?
                                       (get bindings full-fn-name))]
                      (if binding
                        (if-let [ret-tag (:ret (analyze-binding-call ctx full-fn-name binding expr))]
                          (types/add-arg-type-from-expr ctx expr ret-tag)
                          (types/add-arg-type-from-expr ctx expr))
                        (let [id (or (:id expr) (gensym))
                              expr (assoc expr :id id)
                              ret (analyze-call ctx {:arg-count arg-count
                                                     :full-fn-name full-fn-name
                                                     :row row
                                                     :col col
                                                     :expr expr})
                              _ (dorun ret) ;; realize all returned expressions
                                            ;; to not be bitten by laziness
                              maybe-call (some #(when (= id (:id %)) %) ret)]
                          (if (identical? :call (:type maybe-call))
                            (types/add-arg-type-from-call ctx maybe-call expr)
                            (types/add-arg-type-from-expr ctx expr))
                          ret)))
                    (cond
                      (utils/boolean-token? function)
                      (do (reg-not-a-function! ctx expr "boolean")
                          (analyze-children (update ctx :callstack conj [nil t])
                                            (rest children)))
                      (utils/string-from-token function)
                      (do (reg-not-a-function! ctx expr "string")
                          (analyze-children (update ctx :callstack conj [nil t])
                                            (rest children)))
                      (utils/char-token? function)
                      (do (reg-not-a-function! ctx expr "character")
                          (analyze-children (update ctx :callstack conj [nil t])
                                            (rest children)))
                      (utils/number-token? function)
                      (do (reg-not-a-function! ctx expr "number")
                          (analyze-children (update ctx :callstack conj [nil t])
                                            (rest children)))
                      :else
                      (do
                        (types/add-arg-type-from-expr (update ctx :callstack conj [nil t]) expr)
                        (analyze-children ctx children)))))
                ;; catch-all
                (do
                  (types/add-arg-type-from-expr ctx expr)
                  (let [ctx (update ctx :callstack conj [nil t])]
                    (analyze-children ctx children))))))
          (types/add-arg-type-from-expr ctx expr :list))
        :regex (do
                 (lint-unused-value ctx expr)
                 (when (identical? :edn lang)
                   (findings/reg-finding! ctx (assoc (meta expr)
                                                     :filename (:filename ctx)
                                                     :level :error
                                                     :type :syntax
                                                     :message "Regex literals are not allowed in EDN")))
                 (analyze-children (update ctx
                                           :callstack #(cons [nil t] %))
                                   children))
        :vector
        (do
          (lint-unused-value ctx expr)
          (analyze-children (update ctx
                                    :callstack #(cons [nil t] %))
                            children))
        :deref
        (recur ctx (with-meta
                     (seq/list-node [(token/token-node (case lang
                                                         :clj 'clojure.core/deref
                                                         :cljs 'cljs.core/deref
                                                         'clojure.core/deref))
                                     (first (:children expr))])
                     (meta expr)))
        ;; catch-all
        (analyze-children (update ctx
                                  :callstack #(cons [nil t] %))
                          children)))))

;; Hack to make a few functions available in a common namespace without
;; introducing circular depending namespaces. NOTE: alter-var-root! didn't work
;; with GraalVM
(vswap! common assoc
        'analyze-expression** analyze-expression**
        'analyze-children analyze-children
        'analyze-like-let analyze-like-let
        'ctx-with-bindings ctx-with-bindings
        'extract-bindings extract-bindings
        'analyze-defn analyze-defn
        'analyze-usages2 analyze-usages2)

(defn analyze-expression*
  "NOTE: :used-namespaces is used in the cache to load namespaces that were actually used."
  [ctx expression]
  (loop [ctx (assoc ctx
                    :bindings {}
                    :top-level? true)
         ns (:ns ctx)
         [first-parsed & rest-parsed :as all]
         (analyze-expression** ctx expression)]
    (if (seq all)
      (case (:type first-parsed)
        nil (recur ctx ns rest-parsed)
        (:ns :in-ns)
        (let [ns-name (:name first-parsed)
              local-config (:config first-parsed)
              global-config (:global-config ctx)
              new-config (config/merge-config! global-config local-config)]
          (swap! (:used-namespaces ctx) update (:base-lang ctx) into (:used-namespaces first-parsed))
          (recur
           (-> ctx
               (assoc :config new-config)
               (update :top-ns (fn [n]
                                 (or n ns-name))))
           first-parsed
           rest-parsed))
        :import-vars
        (do
          (namespace/reg-proxied-namespaces! ctx (:name ns) (:used-namespaces first-parsed))
          (swap! (:used-namespaces ctx) update (:base-lang ctx) into (:used-namespaces first-parsed))
          (recur ctx
                 ns
                 rest-parsed))
        ;; catch-all
        ;; TODO: don't merge inline-configs we've already seen
        (let [extra-configs (seq @(:inline-configs ctx))
              config (:config ctx)
              config (if extra-configs
                       (apply config/merge-config! config extra-configs)
                       config)]
          ;; TODO: was is java.lang.System here!
          (when-not (:interop? first-parsed)
            (swap! (:used-namespaces ctx) update (:base-lang ctx) conj (:resolved-ns first-parsed)))
          (recur
           (assoc ctx :config config)
           ns
           rest-parsed)))
      (assoc ctx :ns ns))))

(defn analyze-expressions
  "Analyzes expressions and collects defs and calls into a map. To
  optimize cache lookups later on, calls are indexed by the namespace
  they call to, not the ns where the call occurred."
  [{:keys [:base-lang :lang :config] :as ctx}
   expressions]
  (let [init-ns (when-not (= :edn lang)
                  (analyze-ns-decl (-> ctx
                                       (assoc-in [:config :analysis] false)
                                       (dissoc :analysis))
                                   (parse-string "(ns user)")))
        init-ctx (assoc ctx
                        :ns init-ns
                        :calls-by-id (atom {})
                        :top-ns nil
                        :global-config config)]
    (swap! (:used-namespaces ctx)
           update base-lang into (:used-namespaces init-ns))
    (loop [ctx init-ctx
           [expression & rest-expressions] expressions]
      (if expression
        (let [ctx (analyze-expression* ctx expression)]
          (recur ctx rest-expressions))
        nil))))

;;;; processing of string input

(defn- ->findings
  "Convert an exception thrown from rewrite-clj into a sequence clj-kondo :finding"
  [^Exception ex ^String filename]
  (let [{:keys [findings line col] :as d} (ex-data ex)]
    (cond
      (seq findings)
      (for [finding findings]
        (merge {:type :syntax
                :filename filename}
               finding))

      ;; The edn parser in tools.reader throw ex-data with the following shape:
      ;; {:type :reader-exception, :ex-kind :reader-error, :file nil, :line 1, :col 4}
      (identical? :reader-exception (:type d))
      [{:type :syntax
        :filename filename
        :row line
        :col col
        :message (.getMessage ex)}]

      :else
      [{:filename filename
        :col 0
        :row 0
        :type :syntax
        :message (str "Can't parse "
                      filename ", "
                      (or (.getMessage ex) (str ex)))}])))

(defn- lint-line-length [_ctx config filename input]
  (let [findings (atom [])
        line-length-conf (-> config :linters :line-length)]
    (when (not (identical? :off (:level line-length-conf)))
      (when-let [max-line-length (:max-line-length line-length-conf)]
        (let [exclude-urls (:exclude-urls line-length-conf)
              exclude-pattern (:exclude-pattern line-length-conf)
              exclude-pattern (when exclude-pattern
                                (re-pattern exclude-pattern))]
          (with-open [rdr (io/reader (java.io.StringReader. input))]
            (run! (fn [[row line]]
                    (let [line-length (count line)]
                      (when (and (< max-line-length line-length)
                                 (or (not exclude-urls)
                                     (not (str/includes? line "http")))
                                 (or (not exclude-pattern)
                                     (not (re-find exclude-pattern line))))
                        (swap! findings conj {:message (str "Line is longer than " max-line-length " characters.")
                                              :filename filename
                                              :type :line-length
                                              :row (inc row)
                                              :end-row (inc row)
                                              :col (inc max-line-length)
                                              :end-col (count line)}))))
                  (map-indexed vector (line-seq rdr)))))))
    @findings))

(defn analyze-input
  "Analyzes input and returns analyzed defs, calls. Also invokes some
  linters and returns their findings."
  [{:keys [:config :file-analyzed-fn :total-files :files] :as ctx} filename uri input lang dev?]
  (when (:debug ctx)
    (utils/stderr "[clj-kondo] Linting file:" filename))
  (let [ctx (assoc ctx :filename filename)]
    (binding [utils/*ctx* ctx]
      (try
        (let [reader-exceptions (atom [])
              [only-warn-on-interop warn-on-reflect-enabled? :as reflect-opts]
              (when (identical? :clj lang)
                (let [cfg (-> config :linters :warn-on-reflection)]
                  (when-not (identical? :off (:level cfg))
                    (let [has-setting? (str/includes? input "*warn-on-reflection*")
                          only-on-interop (when-not has-setting?
                                            (:warn-only-on-interop cfg))]
                      (when (and (not has-setting?)
                                 (not only-on-interop))
                        (findings/reg-finding!
                         ctx {:message "Var *warn-on-reflection* is not set in this namespace."
                              :filename filename
                              :type :warn-on-reflection
                              :row 1 :col 1 :end-row 1 :end-col 1}))
                      [only-on-interop
                       (str/includes? input "*warn-on-reflection*")]))))
              ctx (if reflect-opts
                    (assoc ctx
                           :warn-on-reflect-enabled warn-on-reflect-enabled?
                           :warn-only-on-interop only-warn-on-interop)
                    ctx)
              ctx (assoc ctx
                         :inline-configs (atom [])
                         :main-ns (atom nil))
              parsed (binding [*reader-exceptions* reader-exceptions]
                       (p/parse-string input))
              fname (fs/file-name filename)
              ctx (case fname
                    ("data_readers.clj"
                     "data_readers.cljc")
                    (utils/ctx-with-linters-disabled
                     (assoc ctx :data-readers true)
                     [:unresolved-symbol :unresolved-namespace :private-call])
                    ctx)
              line-length-findings (lint-line-length ctx config filename input)]
          (doseq [e @reader-exceptions]
            (if dev?
              (throw e)
              (run! #(findings/reg-finding! ctx %) (->findings e filename))))
          (case lang
            :cljc
            (let [cljc-config (:cljc config)
                  features (or (:features cljc-config)
                               [:clj :cljs])]
              (doseq [lang features]
                (analyze-expressions (assoc ctx :base-lang :cljc :lang lang :filename filename)
                                     (:children (select-lang ctx parsed lang)))))
            (:clj :cljs :edn)
            (let [ctx (assoc ctx :base-lang lang :lang lang :filename filename
                             :uri uri)]
              (analyze-expressions ctx (:children parsed))
              ;; analyze-expressions should go first in order to process ignores
              (when (identical? :edn lang)
                (case fname
                  "deps.edn" (deps-edn/lint-deps-edn ctx (first (:children parsed)))
                  "bb.edn" (deps-edn/lint-bb-edn ctx (first (:children parsed)))
                  "config.edn" (when (and (fs/exists? filename)
                                          (= ".clj-kondo"
                                             (-> (fs/parent filename)
                                                 (fs/file-name))))
                                 (lint-config/lint-config ctx (first (:children parsed))))
                  nil))
              (when-let [cfg-dir (-> ctx :config :cfg-dir)]
                (when-let [main-ns @(:main-ns ctx)]
                  (let [configs (-> ctx :inline-configs deref seq)
                        inline-file (io/file cfg-dir "inline-configs"
                                             (str (namespace-munge main-ns)
                                                  (when-let [ext (fs/extension (:filename ctx))]
                                                    (str "." ext))) "config.edn")]
                    (if (and configs (not (false? (:auto-load-configs config))))
                      (binding [cache/*lock-file-name* ".lock"]
                        (cache/with-cache ;; lock config dir for concurrent writes
                          cfg-dir
                          10
                          (spit (doto inline-file
                                  (io/make-parents)) (apply config/merge-config! configs))))
                      (when (fs/exists? inline-file)
                        (fs/delete-tree (fs/parent inline-file)))))))
              (doseq [f line-length-findings]
                (findings/reg-finding! ctx f))
              nil)))
        (catch Exception e
          (if dev?
            (throw e)
            (run! #(findings/reg-finding! ctx %) (->findings e filename))))
        (finally
          (swap! files inc)
          (let [output-cfg (:output config)]
            (when (and (= :text (:format output-cfg))
                       (:progress output-cfg))
              (binding [*out* *err*]
                (print ".") (flush))))
          (when file-analyzed-fn
            (file-analyzed-fn {:filename filename
                               :uri uri
                               :total-files total-files
                               :files-done @files})))))))

;;;; Scratch

(comment
  (parse-string "#js [1 2 3]"))
