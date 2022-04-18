(ns clj-kondo.impl.analyzer.match
  (:require [clj-kondo.impl.analyzer.common :as common]
            [clj-kondo.impl.utils :as utils]))

(defn reg-used-binding!
  [{:keys [:base-lang :lang :namespaces :ns]} binding]
  (swap! namespaces update-in [base-lang lang (:name ns) :used-bindings]
         conj binding)
  nil)

(defn into* [ctx existing-bindings new-bindings]
  (reduce-kv (fn [m k v] (if-let [b (get m k)]
                           (do (reg-used-binding! ctx b)
                               (assoc m k v))
                           (assoc m k v))) existing-bindings  new-bindings))

(defn analyze-token [ctx expr]
  (let [sym (utils/symbol-from-token expr)]
    (if (and sym
             (not (contains? (:bindings ctx) sym)))
      (common/extract-bindings ctx expr)
      (do (common/analyze-expression** ctx expr)
          nil))))

(declare analyze-expr)

(defn analyze-children [ctx expr]
  (let [children (:children expr)]
    (loop [children (seq children)
           bindings {}]
      (if children
        (let [child (first children)]
          (if-let [bnds (analyze-expr ctx child)]
            (recur (next children)
                   (into* ctx bindings bnds))
            (recur (next children) bindings)))
        bindings))))

(defn analyze-list [ctx expr]
  (let [children (:children expr)
        snd (second children)]
    (if-let [k (:k snd)]
      (if (or (identical? :<< k)
              (identical? :when k)
              (identical? :guard k))
        ;; https://github.com/clojure/core.match/blob/fb3188934ab9b6df0249ba3092a888def3434eee/src/main/clojure/clojure/core/match.clj#L1835
        (let [bnds (analyze-expr ctx (first children))]
          (common/analyze-expression** ctx (first (nnext children)))
          bnds)
        (analyze-children ctx expr))
      (analyze-children ctx expr))))

(defn analyze-vector [ctx expr]
  (let [children (:children expr)]
    (loop [children (seq children)
           bindings {}]
      (if children
        (let [[child maybe-op & rchildren] children
              k (:k maybe-op)]
          (if (and k (or (identical? :when k)
                         (identical? :guard k)))
            ;; flattened syntax
            (let [bnds (analyze-expr ctx child)]
              (common/analyze-expression** ctx (first rchildren))
              (into* ctx bnds (analyze-vector ctx {:children (rest rchildren)})))
            (if-let [bnds (analyze-expr ctx child)]
              (recur (next children)
                     (into* ctx bindings bnds))
              (recur (next children) bindings))))
        bindings))))

(defn analyze-expr [ctx expr]
  (let [tag (utils/tag expr)]
    (case tag
      :token
      (analyze-token ctx expr)
      (:list)
      (analyze-list ctx expr)
      (:vector)
      (analyze-vector ctx expr)
      (:map)
      (analyze-children ctx expr)
      ;; default
      (do (common/analyze-expression** ctx expr)
          nil))))

(defn analyze-match [ctx expr]
  (let [[_match pattern & clauses] (:children expr)]
    (common/analyze-expression** ctx pattern)
    (doseq [[clause ret] (partition 2 clauses)]
      (let [bindings (analyze-expr ctx clause)
            ctx (if bindings
                  (utils/ctx-with-bindings ctx bindings)
                  ctx)]
        (common/analyze-expression** ctx ret)))))
