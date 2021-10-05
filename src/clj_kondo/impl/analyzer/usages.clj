(ns clj-kondo.impl.analyzer.usages
  {:no-doc true}
  (:refer-clojure :exclude [ns-name])
  (:require
    [clj-kondo.impl.analysis :as analysis]
    [clj-kondo.impl.analyzer.common :as common]
    [clj-kondo.impl.findings :as findings]
    [clj-kondo.impl.metadata :as meta]
    [clj-kondo.impl.namespace :as namespace]
    [clj-kondo.impl.utils :as utils :refer [tag one-of symbol-from-token kw->sym assoc-some symbol-token?]]
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
                 alias-or-ns)]
    {:name name-sym
     :ns ns-sym
     :namespace-from-prefix (and prefix
                                 (not alias-or-ns)
                                 (not (:namespaced? expr)))
     :alias (when (and aliased? (not= :clj-kondo/unknown-namespace ns-sym)) alias-or-ns)}))

(defn analyze-keyword
  ([ctx expr] (analyze-keyword ctx expr {}))
  ([ctx expr opts]
   (let [ns (:ns ctx)
         ns-name (:name ns)
         keyword-val (:k expr)]
     (when (:analyze-keywords? ctx)
       (let [{:keys [:destructuring-expr :keys-destructuring?]} opts
             current-ns (some-> ns-name symbol)
             destructuring (when destructuring-expr (resolve-keyword ctx destructuring-expr current-ns))
             resolved (resolve-keyword ctx expr current-ns)]
         (analysis/reg-keyword-usage!
           ctx
           (:filename ctx)
           (assoc-some (meta expr)
                       :reg (:reg expr)
                       :keys-destructuring keys-destructuring?
                       :auto-resolved (:namespaced? expr)
                       :namespace-from-prefix (when (:namespace-from-prefix resolved) true)
                       :name (:name resolved)
                       :alias (when-not (:alias destructuring) (:alias resolved))
                       :ns (or (:ns destructuring) (:ns resolved))))))
     (when (and keyword-val (:namespaced? expr))
       (let [symbol-val (kw->sym keyword-val)
             {resolved-ns :ns}
             (namespace/resolve-name ctx ns-name symbol-val)]
         (if resolved-ns
           (namespace/reg-used-namespace! ctx
                                          (-> ns :name)
                                          resolved-ns)
           (namespace/reg-unresolved-namespace! ctx ns-name
                                                (with-meta (symbol (namespace symbol-val))
                                                           (meta expr)))))))))

(defn analyze-namespaced-map [ctx ^NamespacedMapNode expr]
  (let [children (:children expr)
        m (first children)
        ns-name (-> ctx :ns :name)
        the-ns (namespace/get-namespace ctx (:base-lang ctx) (:lang ctx) ns-name)
        ns-keyword (-> expr :ns :k)
        ns-sym (kw->sym ns-keyword)
        aliased? (:aliased? expr)
        resolved-ns (when aliased? (get (:qualify-ns the-ns) ns-sym))
        resolved (or resolved-ns ns-sym)]
    (when resolved-ns
      (when-let [resolved-ns (get (:qualify-ns the-ns) ns-sym)]
        (namespace/reg-used-namespace! ctx
                                       ns-name
                                       resolved-ns)))
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
  ([ctx expr {:keys [:quote? :syntax-quote?] :as opts}]
   (let [ns (:ns ctx)
         dependencies (:dependencies ctx)
         syntax-quote-level (or (:syntax-quote-level ctx) 0)
         ns-name (:name ns)
         t (tag expr)
         quote? (or quote?
                    (= :quote t))
         ;; nested syntax quotes are treated as normal quoted expressions by clj-kondo
         syntax-quote-tag? (= :syntax-quote t)
         unquote-tag? (one-of t [:unquote :unquote-splicing])
         new-syntax-quote-level (cond syntax-quote-tag? (inc syntax-quote-level)
                                      unquote-tag? (dec syntax-quote-level)
                                      :else syntax-quote-level)
         syntax-quote? (or syntax-quote? syntax-quote-tag?)
         ctx (assoc ctx :syntax-quote-level new-syntax-quote-level)]
     (if (and (= 1 syntax-quote-level) unquote-tag?)
       (common/analyze-expression** ctx expr)
       (if quote?
         (do (when (:k expr)
               (analyze-keyword ctx expr opts))
             (doall (mapcat
                     #(analyze-usages2 ctx %
                                       (assoc opts :quote? quote? :syntax-quote? syntax-quote?))
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
                 (if-let [b (when (and simple? (not syntax-quote?))
                              (get (:bindings ctx) symbol-val))]
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
                                                              :str (:string-value expr))))
                   (let [{resolved-ns :ns
                          resolved-name :name
                          resolved-alias :alias
                          unresolved? :unresolved?
                          clojure-excluded? :clojure-excluded?
                          interop? :interop?
                          resolved-core? :resolved-core?
                          :as _m}
                         (let [v (namespace/resolve-name ctx ns-name symbol-val)]
                           (when-not syntax-quote?
                             (when-let [n (:unresolved-ns v)]
                               (namespace/reg-unresolved-namespace!
                                ctx ns-name
                                (with-meta n
                                  (meta expr)))))
                           (if (:unresolved? v)
                             (let [symbol-str (str symbol-val)]
                               (if (str/ends-with? (str symbol-val) ".")
                                 (namespace/resolve-name ctx ns-name
                                                         (symbol (subs symbol-str
                                                                       0 (dec (count symbol-str)))))
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
                       (namespace/reg-var-usage! ctx ns-name
                                                 {:type :use
                                                  :name (with-meta
                                                          resolved-name
                                                          m)
                                                  :resolved-ns resolved-ns
                                                  :ns ns-name
                                                  :alias resolved-alias
                                                  :unresolved? unresolved?
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
                                                  (or syntax-quote?
                                                      ;; e.g. usage of clojure.core, clojure.string, etc in (:require [...])
                                                      (= symbol-val (get (:qualify-ns ns) symbol-val)))
                                                  :private-access? (or syntax-quote? (:private-access? ctx))
                                                  :callstack (:callstack ctx)
                                                  :config (:config ctx)
                                                  :in-def (:in-def ctx)
                                                  :in-reg (:in-reg ctx)
                                                  :simple? simple?
                                                  :interop? interop?
                                                  ;; save some memory
                                                  :expr (when-not dependencies expr)
                                                  :resolved-core? resolved-core?})))))
               (do
                 ;; (prn (type (utils/sexpr expr)) (:callstack ctx) (:len ctx) (:idx ctx))
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
                                  (one-of core-sym [do fn defn defn-
                                                    let when-let loop binding with-open
                                                    doseq try when when-not when-first
                                                    when-some future]))]
                         (when redundant?
                           (findings/reg-finding! ctx (assoc (meta expr)
                                                             :type :redundant-expression
                                                             :message (str "Redundant expression: " (str expr))
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
