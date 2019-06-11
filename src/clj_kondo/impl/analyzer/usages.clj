(ns clj-kondo.impl.analyzer.usages
  (:require
   [clj-kondo.impl.namespace :as namespace :refer [resolve-name]]
   [rewrite-clj.node.protocols :as node]
   [clj-kondo.impl.utils :as utils :refer
    [symbol-call keyword-call node->line
     parse-string parse-string-all tag select-lang
     vconj deep-merge one-of]]))

(defn node->keyword [node]
  (when-let [k (:k node)]
    (and (keyword? k) [:keyword k])))

(defn node->symbol [node]
  (when-let [s (:value node)]
    (and (symbol? s) [:symbol s])))

(defn analyze-usages
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
