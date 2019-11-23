(ns clj-kondo.impl.analyzer.usages
  {:no-doc true}
  (:refer-clojure :exclude [ns-name])
  (:require
   [clj-kondo.impl.namespace :as namespace]
   [clj-kondo.impl.utils :as utils :refer
    [tag one-of symbol-from-token tag kw->sym]]
   [clojure.string :as str])
  (:import [clj_kondo.impl.rewrite_clj.node.seq NamespacedMapNode]))

(set! *warn-on-reflection* true)

(defn analyze-keyword [ctx expr]
  (let [ns (:ns ctx)
        ns-name (:name ns)
        keyword-val (:k expr)]
    (when (:namespaced? expr)
      (let [symbol-val (kw->sym keyword-val)
            {resolved-ns :ns
             _resolved-name :name
             _unresolved? :unresolved? :as _m}
            (namespace/resolve-name ctx ns-name symbol-val)]
        (when resolved-ns
          (namespace/reg-used-namespace! ctx
                                         (-> ns :name)
                                         resolved-ns))))))

(defn analyze-namespaced-map [ctx ^NamespacedMapNode expr]
  (let [children (:children expr)
        m (first children)
        ns (:ns ctx)
        ns-keyword (-> expr :ns :k)
        ns-sym (kw->sym ns-keyword)]
    (when (:aliased? expr)
      (when-let [resolved-ns (get (:qualify-ns ns) ns-sym)]
        (namespace/reg-used-namespace! ctx
                                       (-> ns :name)
                                       resolved-ns)))
    (when-let [f (:analyze-expression** ctx)]
      (f ctx m))))

(defn analyze-usages2
  ([ctx expr] (analyze-usages2 ctx expr {}))
  ([ctx expr {:keys [:quote? :syntax-quote?] :as opts}]
   (let [ns (:ns ctx)
         ns-name (:name ns)
         t (tag expr)
         quote? (or quote? (= :quote t))]
     (if (one-of t [:unquote :unquote-splicing])
       (when-let [f (:analyze-expression** ctx)]
         (f ctx expr))
       (when (or (not quote?)
                 ;; when we're in syntax-quote, we should still look for
                 ;; unquotes, since these will be evaluated first
                 syntax-quote?)
         (let [syntax-quote?
               (or syntax-quote?
                   (= :syntax-quote t))]
           (case t
             :token
             (if-let [symbol-val (symbol-from-token expr)]
               (let [simple-symbol? (empty? (namespace symbol-val))]
                 (if-let [b (when (and simple-symbol? (not syntax-quote?))
                              (get (:bindings ctx) symbol-val))]
                   (namespace/reg-used-binding! ctx
                                                (-> ns :name)
                                                b)
                   (let [{resolved-ns :ns
                          resolved-name :name
                          unresolved? :unresolved?
                          clojure-excluded? :clojure-excluded?
                          :as _m}
                         (let [v (namespace/resolve-name ctx ns-name symbol-val)]
                           (if (:unresolved? v)
                             (let [symbol-str (str symbol-val)]
                               (if (str/ends-with? (str symbol-val) ".")
                                 (namespace/resolve-name ctx ns-name
                                                         (symbol (subs symbol-str
                                                                       0 (dec (count symbol-str)))))
                                 v))
                             v))
                         m (meta expr)
                         {:keys [:row :col]} m]
                     (when resolved-ns
                       (namespace/reg-used-namespace! ctx
                                                      ns-name
                                                      resolved-ns)
                       (namespace/reg-var-usage! ctx ns-name
                                                 {:type :use
                                                  :name resolved-name
                                                  :resolved-ns resolved-ns
                                                  :ns ns-name
                                                  :unresolved? unresolved?
                                                  :clojure-excluded? clojure-excluded?
                                                  :row row
                                                  :col col
                                                  :base-lang (:base-lang ctx)
                                                  :lang (:lang ctx)
                                                  :top-ns (:top-ns ctx)
                                                  :filename (:filename ctx)
                                                  :unresolved-symbol-disabled?
                                                  (or syntax-quote?
                                                      ;; e.g.: clojure.core, clojure.string, etc.
                                                      (= symbol-val (get (:qualify-ns ns) symbol-val)))
                                                  :private-access? (:private-access? ctx)
                                                  :callstack (:callstack ctx)
                                                  :config (:config ctx)})))))
               (when (:k expr)
                 (analyze-keyword ctx expr)))
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
