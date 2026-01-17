(ns clj-kondo.impl.analyzer.usages
  {:no-doc true}
  (:refer-clojure :exclude [ns-name])
  (:require
   [clj-kondo.impl.analysis :as analysis]
   [clj-kondo.impl.analyzer.common :as common]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.metadata :as meta]
   [clj-kondo.impl.namespace :as namespace]
   [clj-kondo.impl.utils :as utils :refer [tag one-of symbol-from-token kw->sym assoc-some
                                           symbol-token?]]
   [clojure.string :as str])
  (:import [clj_kondo.impl.rewrite_clj.node.seq NamespacedMapNode]))

(set! *warn-on-reflection* true)

(defn ^:private resolve-keyword [ctx expr current-ns]
  (let [aliased? (:namespaced? expr)
        token (if (symbol-token? expr)
                (symbol-from-token expr)
                (:k expr))
        prefix (:prefix expr)
        name-sym (some-> token name symbol)
        alias-or-ns (some-> token namespace symbol)
        ns-sym (cond
                 (and aliased? alias-or-ns)
                 (-> (namespace/get-namespace ctx (:base-lang ctx) (:lang ctx) current-ns)
                     (get-in [:aliases alias-or-ns] :clj-kondo/unknown-namespace))

                 aliased?
                 current-ns

                 (and prefix
                      (= '_ alias-or-ns))
                 nil

                 (and prefix
                      (not alias-or-ns))
                 prefix

                 :else
                 alias-or-ns)
        alias (when (and aliased? (not= :clj-kondo/unknown-namespace ns-sym)) alias-or-ns)]
    {:name name-sym
     :ns ns-sym
     :namespace-from-prefix (and prefix
                                 (not alias-or-ns)
                                 (not (:namespaced? expr)))
     :alias alias}))

(defn analyze-keyword
  ([ctx expr] (analyze-keyword ctx expr {}))
  ([ctx expr opts]
   (let [ns (:ns ctx)
         ns-name (:name ns)
         keyword-val (:k expr)]
     (when (:analyze-keywords? ctx)
       (let [{:keys [destructuring-expr keys-destructuring?
                     keys-destructuring-ns-modifier?]} opts
             current-ns (some-> ns-name symbol)
             destructuring (when destructuring-expr (resolve-keyword ctx destructuring-expr
                                                                     current-ns))
             resolved (resolve-keyword ctx expr current-ns)]
         (analysis/reg-keyword-usage!
          ctx
          (:filename ctx)
          (assoc-some (meta expr)
                      :context (utils/deep-merge
                                (:context ctx)
                                (:context expr))
                      :reg (:reg expr)
                      :keys-destructuring keys-destructuring?
                      :keys-destructuring-ns-modifier keys-destructuring-ns-modifier?
                      :auto-resolved (:namespaced? expr)
                      :namespace-from-prefix (when (:namespace-from-prefix resolved) true)
                      :name (:name resolved)
                      :alias (when-not (:alias destructuring) (:alias resolved))
                      :ns (or (:ns destructuring) (:ns resolved))))))
     (when (and keyword-val (:namespaced? expr) (namespace keyword-val))
       (let [symbol-val (kw->sym keyword-val)
             {resolved-ns :ns}
             (namespace/resolve-name ctx false ns-name symbol-val nil)]
         (if resolved-ns
           (namespace/reg-used-namespace! ctx
                                          (-> ns :name)
                                          resolved-ns)
           (namespace/reg-unresolved-namespace! ctx ns-name
                                                (with-meta (symbol (namespace symbol-val))
                                                  (assoc (meta expr)
                                                         :name (-> symbol-val name symbol))))))))))

(defn analyze-namespaced-map [ctx ^NamespacedMapNode expr]
  (let [children (:children expr)
        m (first children)
        ;; reset location of inner map to location of outer expression
        m (vary-meta m merge (meta expr))
        ns-name (-> ctx :ns :name)
        the-ns (namespace/get-namespace ctx (:base-lang ctx) (:lang ctx) ns-name)
        ns-keyword (-> expr :ns :k)
        ns-sym (kw->sym ns-keyword)
        ns-sym (if (= '__current-ns__ ns-sym)
                 (-> ctx :ns :name)
                 ns-sym)
        aliased? (:aliased? expr)
        resolved-ns (if aliased? (get (:qualify-ns the-ns) ns-sym)
                        ns-sym)
        resolved (or resolved-ns ns-sym)]
    (when resolved-ns
      (when aliased? (namespace/reg-used-alias! ctx ns-name ns-sym))
      (namespace/reg-used-namespace! ctx
                                     ns-name
                                     resolved-ns))
    (when-not resolved-ns
      (namespace/reg-unresolved-namespace! ctx
                                           ns-name
                                           (with-meta ns-sym (meta expr))))
    (let [children (:children m)
          keys (take-nth 2 children)
          vals (take-nth 2 (rest children))
          keys  (map (fn [child]
                       (assoc child :prefix resolved)) keys)
          children (interleave keys vals)
          m (assoc m :children children)]
      (when-let [f (:analyze-expression** ctx)]
        (f ctx m)))))

(defn analyze-usages2
  ([ctx expr] (analyze-usages2 ctx expr {}))
  ([ctx expr {:keys [quote? syntax-quote?] :as opts}]
   (let [ns (:ns ctx)
         dependencies (:dependencies ctx)
         syntax-quote-level (or (:syntax-quote-level ctx) 0)
         sintax-quote-level-pos? (pos? syntax-quote-level)
         ns-name (:name ns)
         t (tag expr)
         quote? (or quote?
                    (= :quote t))
         ;; nested syntax quotes are treated as normal quoted expressions by clj-kondo
         syntax-quote-tag? (= :syntax-quote t)
         unquote-tag? (one-of t [:unquote :unquote-splicing])
         new-syntax-quote-level (cond syntax-quote-tag? (inc syntax-quote-level)
                                      :else syntax-quote-level)
         syntax-quote? (or syntax-quote? syntax-quote-tag?)
         ctx (assoc ctx :syntax-quote-level new-syntax-quote-level)
         ctx (if syntax-quote-tag?
               (update ctx :callstack #(cons [:syntax-quote] %))
               ctx)]
     (if (and sintax-quote-level-pos? unquote-tag?)
       (common/analyze-expression** ctx expr)
       (if quote?
         (do
           (when (:k expr)
             (analyze-keyword ctx expr opts))
           (doall (mapcat
                   #(analyze-usages2 ctx %
                                     (assoc opts
                                            :quote? quote?
                                            :syntax-quote? syntax-quote?))
                   (:children expr))))
         (let [syntax-quote?
               (or syntax-quote?
                   (= :syntax-quote t))]
           (meta/lift-meta-content2 ctx expr true)
           (case t
             :token
             (if-let [symbol-val (symbol-from-token expr)]
               (let [simple? (simple-symbol? symbol-val)
                     symbol-val (if simple?
                                  (namespace/normalize-sym-name ctx symbol-val)
                                  symbol-val)
                     expr-meta (meta expr)]
                 (if-let [b (when (and simple? (not sintax-quote-level-pos?))
                              (or (get (:bindings ctx) symbol-val)
                                  (get (:bindings ctx)
                                       (str/replace (str symbol-val) #"\**$" ""))))]
                   (do
                     (when-let [ul (:undefined-locals ctx)]
                       (when (contains? ul symbol-val)
                         (findings/reg-finding! ctx (utils/node->line (:filename ctx)
                                                                      expr
                                                                      :destructured-or-binding-of-same-map
                                                                      (str "Destructured :or refers to binding of same map: "
                                                                           symbol-val)))))
                     (namespace/reg-used-binding! ctx
                                                  (-> ns :name)
                                                  b
                                                  (when (:analyze-locals? ctx)
                                                    (assoc-some expr-meta
                                                                :name-row (:row expr-meta)
                                                                :name-col (:col expr-meta)
                                                                :name-end-row (:end-row expr-meta)
                                                                :name-end-col (:end-col expr-meta)
                                                                :name symbol-val
                                                                :filename (:filename ctx)
                                                                :str (:string-value expr)))))
                   (let [{resolved-ns :ns
                          resolved-name :name
                          resolved-alias :alias
                          unresolved? :unresolved?
                          clojure-excluded? :clojure-excluded?
                          interop? :interop?
                          resolved-core? :resolved-core?
                          :as _m}
                         (let [v (namespace/resolve-name ctx false ns-name symbol-val expr)]
                           (when-not sintax-quote-level-pos?
                             (when-let [n (:unresolved-ns v)]
                               (namespace/reg-unresolved-namespace!
                                ctx ns-name
                                (with-meta n
                                  (assoc (meta expr)
                                         :name (-> symbol-val name symbol))))))
                           (if (:unresolved? v)
                             (let [symbol-str (str symbol-val)]
                               (if (and (not= "." symbol-str)
                                        (str/ends-with? symbol-str "."))
                                 (namespace/resolve-name ctx true ns-name
                                                         (symbol (subs symbol-str
                                                                       0 (dec (count symbol-str))))
                                                         expr)
                                 v))
                             v))
                         m (meta expr)
                         row (:row m)
                         col (:col m)
                         end-row (:end-row m)
                         end-col (:end-col m)]
                     (when resolved-ns
                       ;; this causes the namespace data to be loaded from cache
                       (swap! (:used-namespaces ctx) update (:base-lang ctx) conj resolved-ns)
                       (namespace/reg-used-namespace! ctx
                                                      ns-name
                                                      resolved-ns)
                       (when (:analyze-var-usages? ctx)
                         (let [usage {:type :use
                                      :name (with-meta
                                              resolved-name
                                              m)
                                      :resolved-ns resolved-ns
                                      :ns ns-name
                                      :alias resolved-alias
                                      :defmethod (:defmethod ctx)
                                      :dispatch-val-str (:dispatch-val-str ctx)
                                      :unresolved? unresolved?
                                      :allow-forward-reference? (:in-comment ctx)
                                      :clojure-excluded? clojure-excluded?
                                      :row row
                                      :end-row end-row
                                      :col col
                                      :end-col end-col
                                      :base-lang (:base-lang ctx)
                                      :lang (:lang ctx)
                                      :top-ns (:top-ns ctx)
                                      :filename (:filename ctx)
                                      :unresolved-symbol-disabled?
                                      (or sintax-quote-level-pos?
                                          ;; e.g. usage of clojure.core,
                                          ;; clojure.string, etc in (:require [...])
                                          (= symbol-val (get (:qualify-ns ns)
                                                             symbol-val)))
                                      :private-access? (or sintax-quote-level-pos?
                                                           (:private-access? ctx))
                                      :callstack (:callstack ctx)
                                      :config (:config ctx)
                                      :in-def (:in-def ctx)
                                      :context (:context ctx)
                                      :simple? simple?
                                      :interop? interop?
                                      ;; save some memory
                                      :expr (when-not dependencies expr)
                                      :resolved-core? resolved-core?
                                      :condition (:condition expr)}]
                           (namespace/reg-var-usage! ctx ns-name
                                                     usage)
                           (utils/reg-call ctx usage (:id expr))
                           nil)))

                     nil))
                 ;; this is a symbol, either binding or var reference
                 (when-let [idx (:idx ctx)]
                   (let [len (:len ctx)]
                     (when (< idx (dec len))
                       (let [parent-call (first (:callstack ctx))
                             core? (one-of (first parent-call) [clojure.core cljs.core])
                             core-sym (when core?
                                        (second parent-call))
                             generated? (:clj-kondo.impl/generated expr)
                             redundant?
                             (and (not generated?)
                                  core?
                                  (not (:clj-kondo.impl/generated (meta parent-call)))
                                  (one-of core-sym [do fn fn* defn defn-
                                                    let when-let loop binding with-open
                                                    doseq try when when-not when-first
                                                    when-some future defmethod]))]
                         (when redundant?
                           (findings/reg-finding! ctx (assoc (meta expr)
                                                             :type :unused-value
                                                             :message (str "Unused value: " expr)
                                                             :filename (:filename ctx)))))))))
               (do
                 ;; this everything but a symbol token, including keywords
                 (when-let [idx (:idx ctx)]
                   (let [len (:len ctx)]
                     (when (< idx (dec len))
                       (let [parent-call (first (:callstack ctx))
                             parent-call-ns (first parent-call)
                             core? (one-of parent-call-ns [clojure.core cljs.core])
                             test? (when-not core?
                                     (one-of parent-call-ns [clojure.test cljs.test]))
                             core-sym (second parent-call)
                             generated? (:clj-kondo.impl/generated expr)
                             redundant?
                             (and (not generated?)
                                  (or core? test?)
                                  (not (:clj-kondo.impl/generated (meta parent-call)))
                                  (if core?
                                    (one-of core-sym [do fn fn* defn defn-
                                                      let when-let loop binding with-open
                                                      doseq try when when-not when-first
                                                      when-some future defmethod])
                                    (when test?
                                      (one-of core-sym [deftest]))))]
                         (when redundant?
                           (findings/reg-finding! ctx (assoc (meta expr)
                                                             :type :unused-value
                                                             :message (str "Unused value: " expr)
                                                             :filename (:filename ctx))))))))
                 (when (:k expr)
                   (analyze-keyword ctx expr opts))))
             :reader-macro
             (doall (mapcat
                     #(analyze-usages2 ctx %
                                       (assoc opts :quote? quote? :syntax-quote? syntax-quote?))
                     (rest (:children expr))))
             ;; catch-call
             (doall (mapcat
                     #(analyze-usages2 ctx %
                                       (assoc opts :quote? quote? :syntax-quote? syntax-quote?))
                     (:children expr))))))))))

