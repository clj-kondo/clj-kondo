(ns clj-kondo.impl.analyzer.match
  (:require [clj-kondo.impl.analyzer.common :as common]
            [clj-kondo.impl.utils :as utils]))

(defn analyze-token [ctx expr]
  (if (utils/symbol-token? expr)
    (common/extract-bindings ctx expr)
    (do (common/analyze-expression** ctx expr)
        nil)))

(declare analyze-expr)

(defn analyze-children [ctx expr]
  (let [children (:children expr)]
    (loop [children (seq children)
           bindings {}]
      (if children
        (let [child (first children)]
          (if-let [bnds (analyze-expr ctx child)]
            (recur (next children)
                   (into bindings bnds))
            (recur (next children) bindings)))
        bindings))))

(defn analyze-expr [ctx expr]
  (let [tag (utils/tag expr)]
    (case tag
      :token
      (analyze-token ctx expr)
      (:list :vector)
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
