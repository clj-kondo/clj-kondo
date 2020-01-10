(ns clj-kondo.impl.analyzer
  {:no-doc true}
  (:refer-clojure :exclude [ns-name])
  (:require
   [clj-kondo.impl.analyzer.core-async :as core-async]
   [clj-kondo.impl.analyzer.namespace :as namespace-analyzer
    :refer [analyze-ns-decl]]
   [clj-kondo.impl.analyzer.potemkin :as potemkin]
   [clj-kondo.impl.analyzer.spec :as spec]
   [clj-kondo.impl.analyzer.test :as test]
   [clj-kondo.impl.analyzer.usages :as usages :refer [analyze-usages2]]
   [clj-kondo.impl.config :as config]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.linters :as linters]
   [clj-kondo.impl.linters.keys :as key-linter]
   [clj-kondo.impl.macroexpand :as macroexpand]
   [clj-kondo.impl.metadata :as meta]
   [clj-kondo.impl.namespace :as namespace :refer [resolve-name]]
   [clj-kondo.impl.parser :as p]
   [clj-kondo.impl.profiler :as profiler]
   [clj-kondo.impl.schema :as schema]
   [clj-kondo.impl.types :as types]
   [clj-kondo.impl.utils :as utils :refer
    [symbol-call node->line parse-string tag select-lang deep-merge one-of
     linter-disabled? tag sexpr string-from-token assoc-some ctx-with-bindings]]
   [clojure.string :as str]
   [clj-kondo.impl.analyzer.datalog :as datalog]
   [clj-kondo.impl.analyzer.common :refer [common]]
   [clj-kondo.impl.analyzer.compojure :as compojure]))

(set! *warn-on-reflection* true)

(declare analyze-expression**)

(defn analyze-children
  ([ctx children]
   (analyze-children ctx children true))
  ([{:keys [:callstack :config :top-level?] :as ctx} children add-new-arg-types?]
   ;; (prn callstack)
   (let [top-level? (and top-level?
                         (let [fst (first callstack)]
                           (one-of fst [[clojure.core comment]
                                        [cljs.core comment]])))]
     (when-not (config/skip? config callstack)
       (let [ctx (assoc ctx
                        :top-level? top-level?
                        :arg-types (if add-new-arg-types?
                                     (let [[k v] (first callstack)]
                                       (if (and (symbol? k)
                                                (symbol? v))
                                         (atom [])
                                         nil))
                                     (:arg-types ctx)))]
         (mapcat #(analyze-expression** ctx %) children))))))

(defn analyze-keys-destructuring-defaults [ctx m defaults]
  (let [defaults (into {}
                       (for [[k _v] (partition 2 (:children defaults))
                             :let [sym (:value k)]
                             :when sym]
                         [(:value k) (meta k)]))]
    (doseq [[k v] defaults]
      (when-not (contains? m k)
        (findings/reg-finding!
         (:findings ctx)
         {:message (str k " is not bound in this destructuring form") :level :warning
          :row (:row v)
          :col (:col v)
          :filename (:filename ctx)
          :type :unbound-destructuring-default}))))
  (analyze-children ctx (utils/map-node-vals defaults)))

(defn ctx-with-linter-disabled [ctx linter]
  (assoc-in ctx [:config :linters linter :level] :off))

(defn ctx-with-linters-disabled [ctx linters]
  (let [config (get ctx :config)
        linters-config (get config :linters)
        linters-config (reduce (fn [linters linter]
                                 (assoc-in linters [linter :level] :off))
                               linters-config linters)
        config (assoc config :linters linters-config)
        ctx (assoc ctx :config config)]
    ctx))

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
                     t (when t (types/tag-from-meta t))]
                 (with-meta (into {} v)
                   ;; this is used for checking the return tag of a function body
                   (assoc expr-meta
                          :tag t
                          :tags tags)))
       :namespaced-map (extract-bindings ctx (first (:children expr)) opts)
       :map
       ;; first check even amount of keys + vals
       (do (key-linter/lint-map-keys ctx expr)
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
               res)))
       (findings/reg-finding!
        findings
        (node->line (:filename ctx)
                    expr
                    :error
                    :syntax
                    (str "unsupported binding form " expr)))))))

(defn analyze-in-ns [ctx {:keys [:children] :as expr}]
  (let [{:keys [:row :col]} expr
        ns-name (-> children second :children first :value)
        ns (when ns-name
             (namespace-analyzer/new-namespace
              (:filename ctx)
              (:base-lang ctx)
              (:lang ctx)
              ns-name
              :in-ns
              row col))]
    (namespace/reg-namespace! ctx ns)
    (analyze-children ctx (next children))
    ns))

;;;; function arity

(defn analyze-arity [sexpr]
  (loop [[arg & rest-args] sexpr
         arity 0]
    (if arg
      (if (= '& arg)
        {:min-arity arity
         :varargs? true}
        (recur rest-args
               (inc arity)))
      {:fixed-arity arity})))

(defn analyze-fn-arity [ctx body]
  (if-let [children (not-empty (:children body))]
    (let [arg-vec (first children)
          arg-vec-t (tag arg-vec)]
      (if (not= :vector arg-vec-t)
        (findings/reg-finding! (:findings ctx)
                               (node->line (:filename ctx)
                                           body
                                           :warning
                                           :syntax
                                           "Function arguments should be wrapped in vector."))
        (let [arg-bindings (extract-bindings ctx arg-vec {:fn-args? true})
              {return-tag :tag
               arg-tags :tags} (meta arg-bindings)
              arg-list (sexpr arg-vec)
              arity (analyze-arity arg-list)
              ret {:arg-bindings (dissoc arg-bindings :analyzed)
                   :arity arity
                   :analyzed-arg-vec (:analyzed arg-bindings)
                   :args arg-tags
                   :ret return-tag}]
          ret)))
    (findings/reg-finding! (:findings ctx)
                           (node->line (:filename ctx)
                                       body
                                       :warning
                                       :syntax
                                       "Invalid function body."))))

(defn analyze-pre-post-map [ctx expr]
  (let [children (:children expr)]
    (key-linter/lint-map-keys ctx expr)
    (mapcat (fn [[key-expr value-expr]]
              (let [analyzed-key (analyze-expression** ctx key-expr)
                    analyzed-value (if (= :post (:k key-expr))
                                     (analyze-expression**
                                      (ctx-with-bindings ctx '{% {}}) value-expr)
                                     (analyze-expression** ctx value-expr))]
                (concat analyzed-key analyzed-value)))
            (partition 2 children))))

(defn analyze-fn-body [{:keys [:docstring] :as ctx} body]
  (let [{:keys [:arg-bindings
                :arity :analyzed-arg-vec]
         return-tag :ret
         arg-tags :args} (analyze-fn-arity ctx body)
        ctx (ctx-with-bindings ctx arg-bindings)
        ctx (assoc ctx
                   :recur-arity arity
                   :top-level? false)
        children (:children body)
        body-exprs (rest children)
        first-child (first body-exprs)
        analyzed-first-child
        (let [t (when first-child (tag first-child))]
          (cond (= :map t)
                (analyze-pre-post-map ctx first-child)
                (and (not docstring)
                     (> (count body-exprs) 1)
                     (one-of (tag first-child) [:token :multi-line])
                     (string-from-token first-child))
                (findings/reg-finding! (:findings ctx)
                                       (node->line (:filename ctx)
                                                   first-child
                                                   :warning
                                                   :misplaced-docstring
                                                   "Misplaced docstring."))
                :else (analyze-expression** ctx first-child)))
        body-exprs (rest body-exprs)
        parsed
        (analyze-children ctx body-exprs)]
    (assoc arity
           :parsed
           (concat analyzed-first-child analyzed-arg-vec parsed)
           :ret return-tag
           :args arg-tags)))

(defn fn-bodies [ctx children]
  (loop [[expr & rest-exprs :as exprs] children]
    (when expr
      (let [expr (meta/lift-meta-content2 ctx expr)
            t (tag expr)]
        (case t
          :vector [{:children exprs}]
          :list exprs
          (recur rest-exprs))))))

(defn analyze-defn [{:keys [:ns] :as ctx} expr]
  (let [ns-name (:name ns)
        ;; "my-fn docstring" {:no-doc true} [x y z] x
        [name-node & children] (next (:children expr))
        name-node (when name-node (meta/lift-meta-content2 ctx name-node))
        fn-name (:value name-node)
        call (name (symbol-call expr))
        var-meta (meta name-node)
        meta-node (when-let [fc (first children)]
                    (let [t (tag fc)]
                      (if (= :map t) fc
                          (when (not= :vector t)
                            (when-let [sc (second children)]
                              (when (= :map (tag sc))
                                sc))))))
        ;; use dorun to force evaluation, we don't use the result!
        _ (when meta-node (dorun (analyze-expression** ctx meta-node)))
        var-meta (if meta-node
                   (merge var-meta
                          (sexpr meta-node))
                   var-meta)
        macro? (or (= "defmacro" call)
                   (:macro var-meta))
        deprecated (:deprecated var-meta)
        ctx (if macro?
              (ctx-with-bindings ctx '{&env {}
                                       &form {}})
              ctx)
        private? (or (= "defn-" call)
                     (:private var-meta))
        docstring (string-from-token (first children))
        bodies (fn-bodies ctx children)
        _ (when (empty? bodies)
            (findings/reg-finding! (:findings ctx)
                                   (node->line (:filename ctx)
                                               expr
                                               :warning
                                               :syntax
                                               "Invalid function body.")))
        ;; var is known when making recursive call
        _ (when fn-name
            (namespace/reg-var!
             ctx ns-name fn-name expr {:temp true}))
        parsed-bodies (map #(analyze-fn-body
                             (-> ctx
                                 (assoc :docstring docstring
                                        :in-def fn-name)) %)
                           bodies)
        arities (into {} (map (fn [{:keys [:fixed-arity :varargs? :min-arity :ret :args]}]
                                (let [arg-tags (when (some identity args)
                                                 args)
                                      v (assoc-some {}
                                                    :ret ret :min-arity min-arity
                                                    :args arg-tags)]
                                  (if varargs?
                                    [:varargs v]
                                    [fixed-arity v]))))
                      parsed-bodies)
        fixed-arities (into #{} (filter number?) (keys arities))
        varargs-min-arity (get-in arities [:varargs :min-arity])]
    (when fn-name
      (namespace/reg-var!
       ctx ns-name fn-name expr
       (assoc-some (meta name-node)
                   :macro macro?
                   :private private?
                   :deprecated deprecated
                   :fixed-arities (not-empty fixed-arities)
                   :arities arities
                   :varargs-min-arity varargs-min-arity
                   :doc docstring
                   :added (:added var-meta))))
    (mapcat :parsed parsed-bodies)))

(defn analyze-case [ctx expr]
  (let [exprs (-> expr :children)]
    (loop [[constant expr :as exprs] exprs
           parsed []]
      (if-not expr
        (into parsed (when constant
                       (analyze-expression** ctx constant)))
        (recur
         (nnext exprs)
         (into parsed (analyze-expression** ctx expr)))))))

(defn expr-bindings [ctx binding-vector]
  (->> binding-vector :children
       (take-nth 2)
       (map #(extract-bindings ctx %))
       (reduce deep-merge {})))

(defn analyze-let-like-bindings [ctx binding-vector]
  (let [resolved-as-clojure-var-name (:resolved-as-clojure-var-name ctx)
        for-like? (one-of resolved-as-clojure-var-name [for doseq])
        callstack (:callstack ctx)
        call (-> callstack second second)
        let? (= 'let call)]
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
                   (ctx-with-bindings ctx bindings) value)]
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
                          (cond maybe-call (types/ret-tag-from-call ctx maybe-call value)
                                value {:tag (types/expr->tag ctx* value)})))
                  new-bindings (when binding (extract-bindings ctx* binding tag))
                  analyzed-binding (:analyzed new-bindings)
                  new-bindings (dissoc new-bindings :analyzed)
                  next-arities (if-let [arity (:arity (meta analyzed-value))]
                                 ;; in this case binding-sexpr is a symbol,
                                 ;; since functions cannot be destructured
                                 (assoc arities binding-val arity)
                                 arities)]
              (recur rest-bindings
                     (merge bindings new-bindings)
                     next-arities (concat analyzed analyzed-binding analyzed-value)))))
        {:arities arities
         :bindings bindings
         :analyzed analyzed}))))

(defn lint-even-forms-bindings! [ctx form-name bv]
  (let [num-children (count (:children bv))
        {:keys [:row :col]} (meta bv)]
    (when (odd? num-children)
      (findings/reg-finding!
       (:findings ctx)
       {:type :syntax
        :message (format "%s binding vector requires even number of forms" form-name)
        :row row
        :col col
        :level :error
        :filename (:filename ctx)}))))

(defn assert-vector [{:keys [:filename :findings]} call expr]
  (when expr
    (let [vector? (and expr (= :vector (tag expr)))]
      (if-not vector?
        (do (findings/reg-finding!
             findings
             (node->line filename expr :error :syntax
                         ;; cf. error in clojure
                         (format "%s requires a vector for its binding" call)))
            nil)
        expr))))

(defn analyze-like-let
  [{:keys [:filename :callstack
           :maybe-redundant-let?] :as ctx} expr]
  (let [call (-> callstack first second)
        let? (= 'let call)
        let-parent? (one-of (second callstack)
                            [[clojure.core let]
                             [cljs.core let]])
        bv-node (-> expr :children second)
        valid-bv-node (assert-vector ctx call bv-node)]
    (when (and let? (or (and let-parent? maybe-redundant-let?)
                        (and valid-bv-node (empty? (:children valid-bv-node)))))
      (findings/reg-finding!
       (:findings ctx)
       (node->line filename expr :warning :redundant-let "Redundant let expression.")))
    (when bv-node
      (let [{analyzed-bindings :bindings
             arities :arities
             analyzed :analyzed}
            (analyze-let-like-bindings
             (-> ctx
                 ;; prevent linting redundant let when using let in bindings
                 (update :callstack #(cons [nil :let-bindings] %))) valid-bv-node)
            let-body (nnext (:children expr))
            single-child? (and let? (= 1 (count let-body)))]
        (lint-even-forms-bindings! ctx call valid-bv-node)
        (concat analyzed
                (analyze-children
                 (-> ctx
                     (ctx-with-bindings analyzed-bindings)
                     (update :arities merge arities)
                     (assoc :maybe-redundant-let? single-child?))
                 let-body))))))

(defn analyze-do [{:keys [:filename :callstack] :as ctx} expr]
  (let [parent-call (second callstack)
        core? (one-of (first parent-call) [clojure.core cljs.core])
        core-sym (when core?
                   (second parent-call))
        redundant?
        (and (not= 'fn* core-sym)
             (not= 'let* core-sym)
             (or
              ;; zero or one children
              (< (count (rest (:children expr))) 2)
              (and core?
                   (or
                    ;; explicit do
                    (= 'do core-sym)
                    ;; implicit do
                    (one-of core-sym [fn defn defn-
                                      let loop binding with-open
                                      doseq try])))))]
    (when redundant?
      (findings/reg-finding!
       (:findings ctx)
       (node->line filename expr :warning :redundant-do "redundant do"))))
  (analyze-children ctx (next (:children expr))))

(defn lint-two-forms-binding-vector! [ctx form-name expr]
  (let [num-children (count (:children expr))
        {:keys [:row :col]} (meta expr)]
    (when (not= 2 num-children)
      (findings/reg-finding!
       (:findings ctx)
       {:type :syntax
        :message (format "%s binding vector requires exactly 2 forms" form-name)
        :row row
        :col col
        :filename (:filename ctx)
        :level :error}))))

(defn lint-one-or-two-forms-body! [ctx form-name main-expr body-exprs]
  (let [num-children (count body-exprs)
        {:keys [:row :col]} (meta main-expr)]
    (when-not (or (= 1 num-children)
                  (= 2 num-children))
      (findings/reg-finding!
       (:findings ctx)
       {:type :syntax
        :message (format "%s body requires one or two forms" form-name)
        :row row
        :col col
        :filename (:filename ctx)
        :level :error}))))

(defn analyze-conditional-let [ctx call expr]
  ;; TODO: add check for number of children. I.e. when the first child is
  ;; `if-let`, then the number of childs in the body should be exactly two.
  (let [bv (-> expr :children second)
        vector? (when bv (= :vector (tag bv)))]
    (when vector?
      (let [bindings (expr-bindings ctx bv)
            eval-expr (-> bv :children second)
            body-exprs (-> expr :children nnext)]
        (lint-two-forms-binding-vector! ctx call bv)
        (when (one-of call [if-let if-some])
          (lint-one-or-two-forms-body! ctx call expr body-exprs))
        (concat (:analyzed bindings)
                (analyze-expression** ctx eval-expr)
                (analyze-children (ctx-with-bindings ctx
                                                     (dissoc bindings
                                                             :analyzed))
                                  body-exprs))))))

(defn fn-arity [ctx bodies]
  (let [arities (map #(analyze-fn-arity ctx %) bodies)
        fixed-arities (set (keep (comp :fixed-arity :arity) arities))
        varargs-min-arity (some #(when (:varargs? (:arity %))
                                   (:min-arity (:arity %))) arities)]
    (cond-> {}
      (seq fixed-arities) (assoc :fixed-arities fixed-arities)
      varargs-min-arity (assoc :varargs-min-arity varargs-min-arity))))

(defn analyze-fn [ctx expr]
  (let [children (:children expr)
        ?fn-name (when-let [?name-expr (second children)]
                   (let [n (sexpr ?name-expr)]
                     (when (symbol? n)
                       n)))
        bodies (fn-bodies ctx (next children))
        ;; we need the arity beforehand because this is valid in each body
        arity (fn-arity ctx bodies)
        parsed-bodies
        (map #(analyze-fn-body
               (if ?fn-name
                 (-> ctx
                     (update :bindings conj [?fn-name
                                             (assoc (meta (second children))
                                                    :name ?fn-name
                                                    :filename (:filename ctx))])
                     (update :arities assoc ?fn-name
                             arity))
                 ctx) %) bodies)]
    (with-meta (mapcat :parsed parsed-bodies)
      {:arity arity})))

(defn analyze-alias [ctx expr]
  (let [ns (:ns ctx)
        [alias-expr ns-expr :as children] (rest (:children expr))
        alias-sym (when (= :quote (tag alias-expr))
                    (:value (first (:children alias-expr))))
        ns-sym (when (= :quote (tag alias-expr))
                 (:value (first (:children ns-expr))))]
    (if (and alias-sym (symbol? alias-sym) ns-sym (symbol? ns-sym))
      (namespace/reg-alias! ctx (:name ns) alias-sym ns-sym)
      (analyze-children ctx children))
    (assoc-in ns [:qualify-ns alias-sym] ns-sym)))

(defn analyze-loop [ctx expr]
  (let [bv (-> expr :children second)]
    (when (and bv (= :vector (tag bv)))
      (let [arg-count (let [c (count (:children bv))]
                        (when (even? c)
                          (/ c 2)))]
        (analyze-like-let (assoc ctx
                                 :recur-arity {:fixed-arity arg-count}) expr)))))

(defn analyze-recur [{:keys [:findings :filename :recur-arity] :as ctx} expr]
  (when-not (linter-disabled? ctx :invalid-arity)
    (let [arg-count (count (rest (:children expr)))
          expected-arity
          (or (:fixed-arity recur-arity)
              ;; varargs must be passed as a seq or nil in recur
              (when-let [min-arity (:min-arity recur-arity)]
                (inc min-arity)))]
      (cond
        (not expected-arity)
        (findings/reg-finding!
         findings
         (node->line
          filename
          expr
          :warning
          :unexpected-recur "unexpected recur"))
        (not= expected-arity arg-count)
        (findings/reg-finding!
         findings
         (node->line
          filename
          expr
          :error
          :invalid-arity
          (format "recur argument count mismatch (expected %d, got %d)" expected-arity arg-count)))
        :else nil)))
  (analyze-children ctx (:children expr)))

(defn analyze-letfn [ctx expr]
  (let [fns (-> expr :children second :children)
        name-exprs (map #(-> % :children first) fns)
        ctx (ctx-with-bindings ctx
                               (map (fn [name-expr]
                                      [(:value name-expr)
                                       (assoc (meta name-expr)
                                              :name (:value name-expr)
                                              :filename (:filename ctx))])
                                    name-exprs))
        processed-fns (for [f fns
                            :let [children (:children f)
                                  fn-name (:value (first children))
                                  bodies (fn-bodies ctx (next children))
                                  arity (fn-arity ctx bodies)]]
                        {:name fn-name
                         :arity arity
                         :bodies bodies})
        ctx (reduce (fn [ctx pf]
                      (assoc-in ctx [:arities (:name pf)]
                                (:arity pf)))
                    ctx processed-fns)
        parsed-fns (map #(analyze-fn-body ctx %) (mapcat :bodies processed-fns))
        analyzed-children (analyze-children ctx (->> expr :children (drop 2)))]
    (concat (mapcat (comp :parsed) parsed-fns) analyzed-children)))

(declare analyze-defmethod)

(defn analyze-def [ctx expr]
  ;; (def foo ?docstring ?init)
  (let [children (next (:children expr))
        var-name-node (->> children first (meta/lift-meta-content2 ctx))
        metadata (meta var-name-node)
        var-name (:value var-name-node)
        docstring (when (> (count children) 2)
                    (string-from-token (second children)))]
    (when var-name
      (namespace/reg-var! ctx (-> ctx :ns :name)
                          var-name
                          expr
                          (assoc-some metadata
                                      :doc docstring)))
    (analyze-children (assoc ctx
                             :in-def var-name)
                      (nnext (:children expr)))))

(declare analyze-defrecord)

(defn analyze-schema [ctx fn-sym expr]
  (let [{:keys [:expr :schemas]}
        (schema/expand-schema ctx
                              expr)]
    (concat
     (case fn-sym
       fn (analyze-fn ctx expr)
       def (analyze-def ctx expr)
       defn (analyze-defn ctx expr)
       defmethod (analyze-defmethod ctx expr)
       defrecord (analyze-defrecord ctx expr 'defrecord))
     (analyze-children ctx schemas))))

(defn analyze-binding-call [{:keys [:callstack :config :findings] :as ctx} fn-name expr]
  (let [ns-name (-> ctx :ns :name)]
    (namespace/reg-used-binding! ctx
                                 ns-name
                                 (get (:bindings ctx) fn-name))
    (when-not (config/skip? config :invalid-arity callstack)
      (let [filename (:filename ctx)
            children (:children expr)]
        (when-not (linter-disabled? ctx :invalid-arity)
          (when-let [{:keys [:fixed-arities :varargs-min-arity]}
                     (get (:arities ctx) fn-name)]
            (let [arg-count (count (rest children))]
              (when-not (or (contains? fixed-arities arg-count)
                            (and varargs-min-arity (>= arg-count varargs-min-arity)))
                (findings/reg-finding! findings
                                       (node->line filename expr :error
                                                   :invalid-arity
                                                   (linters/arity-error nil fn-name arg-count fixed-arities varargs-min-arity)))))))
        (analyze-children ctx (rest children))))))

(defn lint-inline-def! [{:keys [:in-def :findings :filename]} expr]
  (when in-def
    (findings/reg-finding!
     findings
     (node->line filename expr :warning :inline-def "inline def"))))

(defn analyze-declare [ctx expr]
  (let [ns-name (-> ctx :ns :name)
        var-name-nodes (next (:children expr))]
    (doseq [var-name-node var-name-nodes]
      (namespace/reg-var! ctx ns-name
                          (->> var-name-node (meta/lift-meta-content2 ctx) :value)
                          expr
                          (assoc (meta expr)
                                 :declared true)))))

(defn analyze-catch [ctx expr]
  (let [[class-expr binding-expr & exprs] (next (:children expr))
        _ (analyze-expression** ctx class-expr) ;; analyze usage for unused import linter
        binding (extract-bindings ctx binding-expr)]
    (analyze-children (ctx-with-bindings ctx binding)
                      exprs)))

(defn analyze-try [ctx expr]
  (loop [[fst-child & rst-children] (next (:children expr))
         analyzed []
         ;; TODO: lint syntax
         _catch-phase false
         _finally-phase false]
    (if fst-child
      (case (symbol-call fst-child)
        catch
        (let [analyzed-catch (analyze-catch ctx fst-child)]
          (recur rst-children (into analyzed analyzed-catch)
                 true false))
        finally
        (recur
         rst-children
         (into analyzed (analyze-children ctx (next (:children fst-child))))
         false false)
        (recur
         rst-children
         (into analyzed (analyze-expression** ctx fst-child))
         false false))
      analyzed)))

(defn analyze-defprotocol [{:keys [:ns] :as ctx} expr]
  ;; for syntax, see https://clojure.org/reference/protocols#_basics
  (let [children (next (:children expr))
        name-node (first children)
        protocol-name (:value name-node)
        ns-name (:name ns)]
    (when protocol-name
      (namespace/reg-var! ctx ns-name protocol-name expr
                          {:defined-by 'clojure.core/defprotocol}))
    (doseq [c (next children)
            :when (= :list (tag c)) ;; skip first docstring
            :let [children (:children c)
                  name-node (first children)
                  name-node (meta/lift-meta-content2 ctx name-node)
                  fn-name (:value name-node)
                  arity-vecs (rest children)
                  fixed-arities (set (keep #(when (= :vector (tag %))
                                              ;; skip last docstring
                                              (count (:children %))) arity-vecs))]]
      (when fn-name
        (namespace/reg-var!
         ctx ns-name fn-name expr (assoc (meta c)
                                         :fixed-arities fixed-arities
                                         :defined-by 'clojure.core/defprotocol))))))

(defn analyze-defrecord
  "Analyzes defrecord, deftype and definterface."
  [{:keys [:ns] :as ctx} expr resolved-as]
  (let [ns-name (:name ns)
        children (:children expr)
        children (next children)
        name-node (first children)
        name-node (meta/lift-meta-content2 ctx name-node)
        metadata (meta name-node)
        metadata (assoc metadata :defined-by (symbol "clojure.core" (str resolved-as)))
        record-name (:value name-node)
        bindings? (not= 'definterface resolved-as)
        binding-vector (when bindings? (second children))
        field-count (when bindings? (count (:children binding-vector)))
        bindings (when bindings? (extract-bindings (assoc ctx
                                                          :skip-reg-binding? true)
                                                   binding-vector))]
    (namespace/reg-var! ctx ns-name record-name expr metadata)
    (when-not (= 'definterface resolved-as)
      ;; TODO: it seems like we can abstract creating defn types into a function,
      ;; so we can also call reg-var there
      (namespace/reg-var! ctx ns-name (symbol (str "->" record-name)) expr
                          (assoc metadata
                                 :fixed-arities #{field-count})))
    (when (= 'defrecord resolved-as)
      (namespace/reg-var! ctx ns-name (symbol (str "map->" record-name))
                          expr (assoc metadata
                                      :fixed-arities #{1}
                                      #_#_:expr expr)))
    (analyze-children (-> ctx
                          (ctx-with-linters-disabled [:invalid-arity
                                                      :unresolved-symbol
                                                      :type-mismatch
                                                      :private-call])
                          (ctx-with-bindings bindings))
                      (nnext children))))

(defn analyze-defmethod [ctx expr]
  (let [children (next (:children expr))
        [method-name-node dispatch-val-node & body-exprs] children
        _ (analyze-usages2 ctx method-name-node)
        bodies (fn-bodies ctx body-exprs)
        analyzed-bodies (map #(analyze-fn-body ctx %) bodies)]
    (concat (analyze-expression** ctx dispatch-val-node)
            (mapcat :parsed analyzed-bodies))))

(defn analyze-areduce [ctx expr]
  (let [children (next (:children expr))
        [array-expr index-binding-expr ret-binding-expr init-expr body] children
        index-binding (extract-bindings ctx index-binding-expr)
        ret-binding (extract-bindings ctx ret-binding-expr)
        bindings (merge index-binding ret-binding)
        analyzed-array-expr (analyze-expression** ctx array-expr)
        analyzed-init-expr (analyze-expression** ctx init-expr)
        analyzed-body (analyze-expression** (ctx-with-bindings ctx bindings) body)]
    (concat analyzed-array-expr analyzed-init-expr analyzed-body)))

(defn analyze-this-as [ctx expr]
  (let [[binding-expr & body-exprs] (next (:children expr))
        binding (extract-bindings ctx binding-expr)]
    (analyze-children (ctx-with-bindings ctx binding)
                      body-exprs)))

(defn analyze-as-> [ctx expr]
  (let [children (next (:children expr))
        [as-expr name-expr & forms-exprs] children
        analyzed-as-expr (analyze-expression** ctx as-expr)
        binding (extract-bindings ctx name-expr)]
    (concat analyzed-as-expr
            (analyze-children (ctx-with-bindings ctx binding)
                              forms-exprs))))

(defn analyze-memfn [ctx expr]
  (analyze-children (ctx-with-linter-disabled ctx :unresolved-symbol)
                    (next (:children expr))))

(defn analyze-empty?
  [ctx expr]
  (let [cs (:callstack ctx)
        not-expr (one-of (second cs) [[clojure.core not] [cljs.core not]])]
    (when not-expr
      (findings/reg-finding!
       (:findings ctx)
       (node->line (:filename ctx) not-expr
                   :warning :not-empty?
                   "use the idiom (seq x) rather than (not (empty? x))")))
    (analyze-children ctx (rest (:children expr)) false)))

(defn analyze-require
  "For now we only support the form (require '[...])"
  [ctx expr]
  (let [ns-name (-> ctx :ns :name)
        children (:children expr)
        require-node (first children)
        children (next children)]
    (doseq [child children]
      (if (= :quote (tag child))
        (when-let [libspec-expr (first (:children child))]
          (let [analyzed
                (namespace-analyzer/analyze-require-clauses ctx ns-name [[require-node [libspec-expr]]])]
            (namespace/reg-required-namespaces! ctx ns-name analyzed)))
        (analyze-children ctx children)))
    (analyze-children ctx children)))

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
  [ctx expr]
  (let [children (rest (:children expr))]
    (when-let [[expr msg linter]
               (case (count children)
                 (0 1) [expr "Too few arguments to if." :syntax]
                 2 [expr "Missing else branch." :if]
                 3 nil
                 [expr "Too many arguments to if." :syntax])]
      (findings/reg-finding!
       (:findings ctx)
       (node->line (:filename ctx) expr
                   :warning linter
                   msg)))
    (analyze-children ctx children)))

(defn reg-call [{:keys [:calls-by-id]} call id]
  (swap! calls-by-id assoc id call)
  nil)

(defn analyze-constructor
  "Analyzes (new Foo ...) constructor call."
  [ctx expr]
  (let [[_ ctor-node & children] (:children expr)]
    (analyze-usages2 (ctx-with-linter-disabled ctx :unresolved-symbol) ctor-node)
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
  (let [children (next (:children expr))
        binding-vector (assert-vector ctx 'with-redefs (first children))
        _ (when binding-vector
            (lint-even-forms-bindings! ctx 'with-redefs binding-vector))
        bindings (:children binding-vector)
        lhs (take-nth 2 bindings)
        rhs (take-nth 2 (rest bindings))
        body (next children)]
    (analyze-children (ctx-with-linter-disabled ctx :private-call)
                      lhs)
    (analyze-children ctx rhs)
    (analyze-children ctx body)))

(defn analyze-def-catch-call [ctx expr]
  (let [ns-name (-> ctx :ns :name)
        children (next (:children expr))
        name-expr (->> (first children)
                       (meta/lift-meta-content2 ctx)
                       :value)
        body (next children)
        ctx (ctx-with-linters-disabled ctx [:invalid-arity
                                            :unresolved-symbol
                                            :type-mismatch
                                            :private-call])]
    (namespace/reg-var! ctx ns-name
                        (meta/lift-meta-content2 ctx name-expr)
                        (meta expr))
    (run! #(analyze-usages2 ctx %) body)))

(defn analyze-call
  [{:keys [:top-level? :base-lang :lang :ns :config] :as ctx}
   {:keys [:arg-count
           :full-fn-name
           :row :col
           :expr] :as m}]
  (let [ns-name (:name ns)
        children (next(:children expr))
        {resolved-namespace :ns
         resolved-name :name
         unresolved? :unresolved?
         unresolved-ns :unresolved-ns
         clojure-excluded? :clojure-excluded?
         :as _m}
        (resolve-name ctx ns-name full-fn-name)]
    (cond (and unresolved?
               (str/ends-with? full-fn-name "."))
          (recur ctx
                 (let [expr (macroexpand/expand-dot-constructor ctx expr)]
                   (assoc m
                          :expr expr
                          :full-fn-name 'new
                          :arg-count (inc (:arg-count m)))))
          unresolved-ns
          (do
            (namespace/reg-unresolved-namespace! ctx ns-name
                                                 (with-meta unresolved-ns
                                                   (meta full-fn-name)))
            (analyze-children ctx children))
          :else
          (let [[resolved-as-namespace resolved-as-name _lint-as?]
                (or (when-let
                        [[ns n]
                         (config/lint-as config
                                         [resolved-namespace resolved-name])]
                      [ns n true])
                    [resolved-namespace resolved-name false])
                fq-sym (when (and resolved-namespace
                                  resolved-name)
                         (symbol (str resolved-namespace)
                                 (str resolved-name)))
                unknown-ns? (= :clj-kondo/unknown-namespace resolved-namespace)
                resolved-namespace* (if unknown-ns?
                                      ns-name resolved-namespace)
                ctx (if fq-sym
                      (update ctx :callstack
                              (fn [cs]
                                (cons (with-meta [resolved-namespace* resolved-name]
                                        (meta expr)) cs)))
                      ctx)
                resolved-as-clojure-var-name
                (when (one-of resolved-as-namespace [clojure.core cljs.core])
                  resolved-as-name)
                arg-types (if (and resolved-namespace resolved-name
                                   (not (linter-disabled? ctx :type-mismatch)))
                            (atom [])
                            nil)
                ctx (assoc ctx :arg-types arg-types)
                ctx (if resolved-as-clojure-var-name
                      (assoc ctx
                             :resolved-as-clojure-var-name resolved-as-clojure-var-name)
                      ctx)
                analyzed
                (case resolved-as-clojure-var-name
                  ns
                  (when top-level?
                    [(analyze-ns-decl ctx expr)])
                  in-ns (if top-level? [(analyze-in-ns ctx expr)]
                            (analyze-children ctx children))
                  alias
                  [(analyze-alias ctx expr)]
                  declare (analyze-declare ctx expr)
                  (def defonce defmulti goog-define)
                  (do (lint-inline-def! ctx expr)
                      (analyze-def ctx expr))
                  (defn defn- defmacro definline)
                  (do (lint-inline-def! ctx expr)
                      (analyze-defn ctx expr))
                  defmethod (analyze-defmethod ctx expr)
                  defprotocol (analyze-defprotocol ctx expr)
                  (defrecord deftype definterface) (analyze-defrecord ctx expr resolved-as-clojure-var-name)
                  comment
                  (analyze-children ctx children)
                  (-> some->)
                  (analyze-expression** ctx (macroexpand/expand-> ctx expr))
                  (->> some->>)
                  (analyze-expression** ctx (macroexpand/expand->> ctx expr))
                  doto
                  (analyze-expression** ctx (macroexpand/expand-doto ctx expr))
                  (. .. proxy extend-protocol reify
                     defcurried extend-type)
                  ;; don't lint calls in these expressions, only register them as used vars
                  (analyze-children (ctx-with-linters-disabled ctx [:invalid-arity
                                                                    :unresolved-symbol
                                                                    :type-mismatch])
                                    children)
                  (cond-> cond->>)
                  (analyze-expression** ctx (macroexpand/expand-cond-> ctx expr))
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
                  (use require)
                  (if top-level? (analyze-require ctx expr)
                      (analyze-children ctx children))
                  import
                  (if top-level? (analyze-import ctx expr)
                      (analyze-children ctx children))
                  if (analyze-if ctx expr)
                  new (analyze-constructor ctx expr)
                  set! (analyze-set! ctx expr)
                  with-redefs (analyze-with-redefs ctx expr)
                  ;; catch-all
                  (case [resolved-as-namespace resolved-as-name]
                    [clj-kondo.lint-as def-catch-all]
                    (analyze-def-catch-call ctx expr)
                    [schema.core fn]
                    (analyze-schema ctx 'fn expr)
                    [schema.core def]
                    (analyze-schema ctx 'def expr)
                    [schema.core defn]
                    (analyze-schema ctx 'defn expr)
                    [schema.core defmethod]
                    (analyze-schema ctx 'defmethod expr)
                    [schema.core defrecord]
                    (analyze-schema ctx 'defrecord expr)
                    ([clojure.test deftest]
                     [cljs.test deftest]
                     #_[:clj-kondo/unknown-namespace deftest])
                    (do (lint-inline-def! ctx expr)
                        (test/analyze-deftest ctx resolved-namespace expr))
                    [cljs.test async]
                    (test/analyze-cljs-test-async ctx expr)
                    ([clojure.test are] [cljs.test are] #_[clojure.template do-template])
                    (test/analyze-are ctx expr)
                    ([clojure.spec.alpha fdef] [cljs.spec.alpha fdef])
                    (spec/analyze-fdef (assoc ctx
                                              :analyze-children
                                              analyze-children) expr)
                    [potemkin import-vars]
                    (potemkin/analyze-import-vars ctx expr)
                    ([clojure.core.async alt!] [clojure.core.async alt!!]
                     [cljs.core.async alt!] [cljs.core.async alt!!])
                    (core-async/analyze-alt! (assoc ctx
                                                    :analyze-expression** analyze-expression**
                                                    :extract-bindings extract-bindings)
                                             expr)
                    ([datahike.api q]
                     [datascript.core q]
                     [datomic.api q]
                     [datomic.client.api q])
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
                    ;; catch-all
                    (let [next-ctx (cond-> ctx
                                     (= '[clojure.core.async thread]
                                        [resolved-namespace resolved-name])
                                     (assoc-in [:recur-arity :fixed-arity] 0))]
                      (analyze-children next-ctx children false))))]
            (if (= 'ns resolved-as-clojure-var-name)
              analyzed
              (let [in-def (:in-def ctx)
                    id (:id expr)
                    call (cond-> {:type :call
                                  :resolved-ns resolved-namespace
                                  :ns ns-name
                                  :name (with-meta
                                          (or resolved-name full-fn-name)
                                          (meta full-fn-name))
                                  :unresolved? unresolved?
                                  :unresolved-ns unresolved-ns
                                  :clojure-excluded? clojure-excluded?
                                  :arity arg-count
                                  :row row
                                  :col col
                                  :base-lang base-lang
                                  :lang lang
                                  :filename (:filename ctx)
                                  :expr expr
                                  :callstack (:callstack ctx)
                                  :config (:config ctx)
                                  :top-ns (:top-ns ctx)
                                  :arg-types (:arg-types ctx)}
                           id (assoc :id id)
                           in-def (assoc :in-def in-def))]
                (when id (reg-call ctx call id))
                (namespace/reg-var-usage! ctx ns-name call)
                (when-not unresolved?
                  (namespace/reg-used-namespace! ctx
                                                 ns-name
                                                 resolved-namespace))
                (if-let [m (meta analyzed)]
                  (with-meta (cons call analyzed)
                    m)
                  (cons call analyzed))))))))

(defn lint-keyword-call! [{:keys [:callstack :config :findings] :as ctx} kw namespaced? arg-count expr]
  (when-not (config/skip? config :invalid-arity callstack)
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
                         (namespace kw))
          kw-str (if ?resolved-ns (str ?resolved-ns "/" (name kw))
                     (str (name kw)))]
      (when (or (zero? arg-count)
                (> arg-count 2))
        (findings/reg-finding! findings
                               (node->line (:filename ctx) expr :error :invalid-arity
                                           (format "keyword :%s is called with %s args but expects 1 or 2"
                                                   kw-str
                                                   arg-count)))))))

(defn lint-map-call! [{:keys [:callstack :config
                              :findings] :as ctx} _the-map arg-count expr]
  (when-not (config/skip? config :invalid-arity callstack)
    (when (or (zero? arg-count)
              (> arg-count 2))
      (findings/reg-finding!
       findings
       (node->line (:filename ctx) expr :error :invalid-arity
                   (format "map is called with %s args but expects 1 or 2"
                           arg-count))))))

(defn lint-symbol-call! [{:keys [:callstack :config :findings] :as ctx} _the-symbol arg-count expr]
  (when-not (config/skip? config :invalid-arity callstack)
    (when (or (zero? arg-count)
              (> arg-count 2))
      (findings/reg-finding!
       findings
       (node->line (:filename ctx) expr :error :invalid-arity
                   (format "symbol is called with %s args but expects 1 or 2"
                           arg-count))))))

(defn reg-not-a-function! [{:keys [:filename :callstack
                                   :config :findings]} expr type]
  (when-not (config/skip? config :not-a-function callstack)
    (findings/reg-finding!
     findings
     (node->line filename expr :error :not-a-function (str "a " type " is not a function")))))

(defn analyze-reader-macro [ctx expr]
  (analyze-children ctx (rest (:children expr))))

(defn analyze-expression**
  [{:keys [:bindings :lang] :as ctx}
   {:keys [:children] :as expr}]
  (when expr
    (let [expr (if (or (not= :edn lang)
                       (:quoted ctx))
                 (meta/lift-meta-content2 ctx expr)
                 expr)
          t (tag expr)
          {:keys [:row :col]} (meta expr)
          arg-count (count (rest children))]
      (when-not (one-of t [:map :list :quote]) ;; list and quote are handled specially because of return types
        (types/add-arg-type-from-expr ctx expr))
      (case t
        :quote (let [ctx (assoc ctx :quoted true)]
                 (types/add-arg-type-from-expr ctx (first (:children expr)))
                 (analyze-children ctx children))
        :syntax-quote (analyze-usages2 (assoc ctx :arg-types nil) expr)
        :var (analyze-children (assoc ctx :private-access? true)
                               (:children expr))
        :reader-macro (analyze-reader-macro ctx expr)
        (:unquote :unquote-splicing)
        (analyze-children ctx children)
        :namespaced-map (usages/analyze-namespaced-map
                         (-> ctx
                             (assoc :analyze-expression**
                                    analyze-expression**)
                             (update :callstack #(cons [nil t] %)))
                         expr)
        :map (do (key-linter/lint-map-keys ctx expr)
                 (let [children (map (fn [c s]
                                       (assoc c :id s))
                                     children
                                     (repeatedly #(gensym)))
                       analyzed (analyze-children
                                 (update ctx
                                         :callstack #(cons [nil t] %)) children)]
                   (types/add-arg-type-from-expr ctx (assoc expr
                                                            :children children
                                                            :analyzed analyzed))
                   analyzed))
        :set (do (key-linter/lint-set ctx expr)
                 (analyze-children (update ctx
                                           :callstack #(cons [nil t] %))
                                   children))
        :fn (recur (assoc ctx :arg-types nil)
                   (macroexpand/expand-fn expr))
        :token (when-not (or (:quoted ctx) (= :edn (:lang ctx))) (analyze-usages2 ctx expr))
        :list
        (if-let [function (first children)]
          (if (or (:quoted ctx) (= :edn (:lang ctx)))
            (analyze-children ctx children)
            (let [t (tag function)]
              (case t
                :map
                (do (lint-map-call! ctx function arg-count expr)
                    (types/add-arg-type-from-expr ctx expr)
                    (analyze-children ctx children))
                :quote
                (let [quoted-child (-> function :children first)]
                  (if (utils/symbol-token? quoted-child)
                    (do (lint-symbol-call! ctx quoted-child arg-count expr)
                        (analyze-children ctx children))
                    (do (types/add-arg-type-from-expr ctx expr)
                        (analyze-children ctx children))))
                :token
                (if-let [k (:k function)]
                  (do (lint-keyword-call! ctx k (:namespaced? function) arg-count expr)
                      (types/add-arg-type-from-expr ctx expr)
                      (analyze-children ctx children))
                  (if-let [full-fn-name (let [s (utils/symbol-from-token function)]
                                          (when-not (one-of s ['. '..])
                                            s))]
                    (let [full-fn-name (with-meta full-fn-name (meta function))
                          unresolved? (nil? (namespace full-fn-name))
                          binding-call? (and unresolved?
                                             (contains? bindings full-fn-name))]
                      (if binding-call?
                        (do
                          (types/add-arg-type-from-expr ctx expr)
                          (analyze-binding-call ctx full-fn-name expr))
                        (let [ret (analyze-call ctx {:arg-count arg-count
                                                     :full-fn-name full-fn-name
                                                     :row row
                                                     :col col
                                                     :expr expr})
                              maybe-call (first ret)]
                          (if (= :call (:type maybe-call))
                            (types/add-arg-type-from-call ctx maybe-call expr)
                            (types/add-arg-type-from-expr ctx expr))
                          ret)))
                    (cond
                      (utils/boolean-token? function)
                      (do (reg-not-a-function! ctx expr "boolean")
                          (analyze-children ctx (rest children)))
                      (utils/string-from-token function)
                      (do (reg-not-a-function! ctx expr "string")
                          (analyze-children ctx (rest children)))
                      (utils/char-token? function)
                      (do (reg-not-a-function! ctx expr "character")
                          (analyze-children ctx (rest children)))
                      (utils/number-token? function)
                      (do (reg-not-a-function! ctx expr "number")
                          (analyze-children ctx (rest children)))
                      :else
                      (do
                        ;; (prn "--")
                        (types/add-arg-type-from-expr ctx expr)
                        (analyze-children ctx children)))))
                ;; catch-call
                (do
                  ;; (prn "--" expr (types/add-arg-type-from-expr ctx expr))
                  (types/add-arg-type-from-expr ctx expr)
                  (analyze-children ctx children)))))
          (types/add-arg-type-from-expr ctx expr :list))
        ;; catch-all
        (do
          ;; (prn "--")
          nil
          (analyze-children (update ctx
                                    :callstack #(cons [nil t] %))
                            children))))))

;; Hack to make a few functions available in a common namespace without
;; introducing circular depending namespaces. NOTE: alter-var-root! didn't work
;; with GraalVM
(vreset! common {'analyze-expression** analyze-expression**
                 'analyze-children analyze-children
                 'ctx-with-bindings ctx-with-bindings
                 'extract-bindings extract-bindings
                 'analyze-defn analyze-defn})

(defn analyze-expression*
  "NOTE: :used-namespaces is used in the cache to load namespaces that were actually used."
  [ctx results expression]
  (loop [ctx (assoc ctx
                    :bindings {}
                    :top-level? true)
         ns (:ns ctx)
         [first-parsed & rest-parsed :as all] (analyze-expression** ctx expression)
         results results]
    (if (seq all)
      (case (:type first-parsed)
        nil (recur ctx ns rest-parsed results)
        (:ns :in-ns)
        (let [ns-name (:name first-parsed)
              local-config (:config first-parsed)
              global-config (:global-config ctx)
              new-config (config/merge-config! global-config local-config)]
          (recur
           (-> ctx
               (assoc :config new-config)
               (update :top-ns (fn [n]
                                 (or n ns-name))))
           first-parsed
           rest-parsed
           (-> results
               (assoc :ns first-parsed)
               (update :used-namespaces into (:used-namespaces first-parsed))
               (update :required into (:required first-parsed)))))
        :import-vars
        (do
          (namespace/reg-proxied-namespaces! ctx (:name ns) (:used-namespaces first-parsed))
          (recur ctx
                 ns
                 rest-parsed
                 (update results :used-namespaces into (:used-namespaces first-parsed))))
        ;; catch-all
        (recur
         ctx
         ns
         rest-parsed
         (case (:type first-parsed)
           :call
           (update results :used-namespaces conj (:resolved-ns first-parsed))
           results)))
      [(assoc ctx :ns ns) results])))

(defn analyze-expressions
  "Analyzes expressions and collects defs and calls into a map. To
  optimize cache lookups later on, calls are indexed by the namespace
  they call to, not the ns where the call occurred. Also collects
  other findings and passes them under the :findings key."
  [{:keys [:base-lang :lang :config] :as ctx}
   expressions]
  (profiler/profile
   :analyze-expressions
   (let [init-ns (when-not (= :edn lang)
                   (analyze-ns-decl (assoc-in ctx
                                              [:config :output :analysis] false)
                                    (parse-string "(ns user)")))
         init-ctx (assoc ctx
                         :ns init-ns
                         :calls-by-id (atom {})
                         :top-ns nil
                         :global-config config)]
     (loop [ctx init-ctx
            [expression & rest-expressions] expressions
            results {:required (:required init-ns)
                     :used-namespaces (:used-namespaces init-ns)
                     :findings []
                     :lang base-lang}]
       (if expression
         (let [[ctx results]
               (analyze-expression* ctx results expression)]
           (recur ctx rest-expressions results))
         results)))))

;;;; processing of string input

(defn- ->findings
  "Convert an exception thrown from rewrite-clj into a sequence clj-kondo :finding"
  [^Exception ex ^String filename]
  (let [{:keys [findings line col type]} (ex-data ex)]
    (cond
      (seq findings)
      (for [finding findings]
        (merge {:type :syntax
                :level :error
                :filename filename}
               finding))

      ;; The edn parser in tools.reader throw ex-data with the following shape:
      ;; {:type :reader-exception, :ex-kind :reader-error, :file nil, :line 1, :col 4}
      (= :reader-exception type)
      [{:type :syntax
        :level :error
        :filename filename
        :row line
        :col col
        :message (.getMessage ex)}]

      :else
      [{:level :error
        :filename filename
        :col 0
        :row 0
        :type :syntax
        :message (str "Can't parse "
                      filename ", "
                      (or (.getMessage ex) (str ex)))}])))

(defn analyze-input
  "Analyzes input and returns analyzed defs, calls. Also invokes some
  linters and returns their findings."
  [{:keys [:config] :as ctx} filename input lang dev?]
  ;; (prn "FILENAME" filename)
  (try
    (let [parsed (p/parse-string input)
          analyzed-expressions
          (case lang
            :cljc
            (let [clj (analyze-expressions (assoc ctx :base-lang :cljc :lang :clj :filename filename)
                                           (:children (select-lang parsed :clj)))
                  cljs (analyze-expressions (assoc ctx :base-lang :cljc :lang :cljs :filename filename)
                                            (:children (select-lang parsed :cljs)))]
              (profiler/profile :deep-merge
                                (deep-merge clj cljs)))
            (:clj :cljs :edn)
            (analyze-expressions (assoc ctx :base-lang lang :lang lang :filename filename)
                                 (:children parsed)))]
      analyzed-expressions)
    (catch Exception e
      (if dev?
        (throw e)
        {:findings (->findings e filename)}))
    (finally
      (let [output-cfg (:output config)]
        (when (and (= :text (:format output-cfg))
                   (:progress output-cfg))
          (print ".") (flush))))))

;;;; Scratch

(comment
  (parse-string "#js [1 2 3]")
  )
