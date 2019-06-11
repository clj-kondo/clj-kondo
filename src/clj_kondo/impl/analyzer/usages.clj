(ns clj-kondo.impl.analyzer.usages
  (:require
   [clj-kondo.impl.namespace :as namespace :refer [resolve-name]]
   [rewrite-clj.node.protocols :as node]
   [clj-kondo.impl.utils :as utils :refer
    [symbol-call keyword-call node->line
     parse-string parse-string-all tag select-lang
     vconj deep-merge one-of symbol-from-token]]))

#_(defn node->keyword [node]
  (when-let [k (:k node)]
    (and (keyword? k) [:keyword k])))

#_(defn node->symbol [node]
  (when-let [s (:value node)]
    (and (symbol? s) [:symbol s])))

#_(defn analyze-usages
  ([ctx expr] (analyze-usages ctx false expr))
  ([ctx syntax-quote? expr]
   (let [ns (:ns ctx)
         tag (node/tag expr)
         syntax-quote? (when-not (one-of tag [:unquote :unquote-splicing])
                         (or syntax-quote?
                             (= :syntax-quote tag)))]
     (if-let [[t v] (or (node->keyword expr)
                        (node->symbol expr))]
       (if-let [?ns (namespace v)]
         (let [ns-sym (symbol ?ns)]
           (when-let [resolved-ns (get (:qualify-ns ns) ns-sym)]
             (namespace/reg-usage! ctx
                                   (-> ns :name)
                                   resolved-ns)))
         (when (and (= t :symbol))
           (if-let [b (when-not syntax-quote?
                        (get (:bindings ctx) v))]
             (namespace/reg-used-binding! ctx
                                          (-> ns :name)
                                          b)
             (let [resolved-ns (or (:ns (get (:qualify-var ns) v))
                                   (get (:qualify-ns ns) v))]
               (namespace/reg-usage! ctx
                                     (-> ns :name)
                                     resolved-ns)))))
       (mapcat #(analyze-usages ctx syntax-quote? %)
               (:children expr))))))

(defn analyze-usages2
  ([ctx expr] (analyze-usages2 ctx expr {}))
  ([ctx expr {:keys [:syntax-quote?] :as opts}]
   (let [ns (:ns ctx)
         ns-name (:name ns)
         tag (node/tag expr)
         syntax-quote? (when-not (one-of tag [:unquote :unquote-splicing])
                         (or syntax-quote?
                             (= :syntax-quote tag)))]
     (if (= :token tag)
       (if-let [symbol-val (symbol-from-token expr)]
         (let [simple-symbol? (empty? (namespace symbol-val))]
           (if-let [b (when (and simple-symbol? (not syntax-quote?))
                        (get (:bindings ctx) symbol-val))]
             (namespace/reg-used-binding! ctx
                                          (-> ns :name)
                                          b)
             (if-let [resolved-ns (when simple-symbol?
                                    (get (:qualify-ns ns) symbol-val))]
               (namespace/reg-usage! ctx
                                     (-> ns :name)
                                     resolved-ns)
               (let [{resolved-ns :ns
                      _resolved-name :name
                      unqualified? :unqualified? :as _m} (namespace/resolve-name ctx ns-name symbol-val)]
                 (when unqualified?
                   (namespace/reg-unresolved-symbol! ctx ns-name symbol-val (meta expr)))
                 (when resolved-ns
                   (namespace/reg-usage! ctx
                                         (-> ns :name)
                                         resolved-ns))))))
         (when-let [keyword-val (:k expr)]
           (let [symbol-val (symbol keyword-val)
                 {resolved-ns :ns
                  _resolved-name :name
                  _unqualified? :unqualified? :as _m}
                 (namespace/resolve-name ctx ns-name symbol-val)]
             (when resolved-ns
               (namespace/reg-usage! ctx
                                     (-> ns :name)
                                     resolved-ns)))))
       (mapcat #(analyze-usages2 ctx (assoc opts :syntax-quote? syntax-quote?) %)
               (:children expr))))))
