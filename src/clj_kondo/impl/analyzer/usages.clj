(ns clj-kondo.impl.analyzer.usages
  {:no-doc true}
  (:require
   [clj-kondo.impl.namespace :as namespace]
   [clj-kondo.impl.utils :as utils :refer
    [tag one-of symbol-from-token tag kw->sym]]))

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
          (namespace/reg-usage! ctx
                                (-> ns :name)
                                resolved-ns))))))

(defn analyze-usages2
  ([ctx expr] (analyze-usages2 ctx expr {}))
  ([ctx expr {:keys [:quote? :syntax-quote?] :as opts}]
   (let [ns (:ns ctx)
         ns-name (:name ns)
         tag (tag expr)
         quote? (or quote? (= :quote tag))]
     (if (one-of tag [:unquote :unquote-splicing])
       (when-let [f (:analyze-expression** ctx)]
         (f ctx expr))
       (when (or (not quote?)
                 ;; when we're in syntax-quote, we should still look for
                 ;; unquotes, since these will be evaluated first
                 syntax-quote?)
         (let [syntax-quote?
               (or syntax-quote?
                   (= :syntax-quote tag))]
           (case tag
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
                         (namespace/resolve-name ctx ns-name symbol-val)
                         m (meta expr)
                         {:keys [:row :col]} m]
                     (when resolved-ns
                       (namespace/reg-usage! ctx
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
                                                  :filename (:filename ctx)
                                                  :unresolved-symbol-disabled?
                                                  (or syntax-quote?
                                                      (when simple-symbol?
                                                        (get (:qualify-ns ns) symbol-val)))
                                                  :private-access? (:private-access? ctx)
                                                  :callstack (:callstack ctx)
                                                  :config (:config ctx)})))))
               (when (:k expr)
                 (analyze-keyword ctx expr)))
             ;; catch-call
             (doall (mapcat
                     #(analyze-usages2 ctx % (assoc opts :quote? quote? :syntax-quote? syntax-quote?))
                     (:children expr))))))))))
