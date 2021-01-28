(ns clj-kondo.impl.analyzer.match
  (:require [clj-kondo.impl.analyzer.common :as common]
            [clj-kondo.impl.utils :as utils]))

(defn vector-bindings [ctx expr]
  (let [children (:children expr)]
    (loop [children (seq children)
           bindings {}]
      (if children
        (let [child (first children)]
          (cond (utils/symbol-token? child)
                (recur (next children)
                       (into bindings
                             (common/extract-bindings ctx child)))
                (identical? :vector (utils/tag child))
                (let [nested (vector-bindings ctx child)]
                  (recur (next children)
                         (into bindings nested)))
                :else
                (do (common/analyze-expression** ctx child)
                (recur (next children)
                       bindings))))
        bindings))))

(defn analyze-clause [ctx clause ret]
  (case (utils/tag clause)
    :vector
    (let [bindings (vector-bindings ctx clause)
          ctx (utils/ctx-with-bindings ctx bindings)]
      (common/analyze-expression** ctx ret))
    :list
    
    (do
      (common/analyze-expression** ctx clause)
      (common/analyze-expression** ctx ret))))

(defn analyze-match [ctx expr]
  (let [[_match pattern & clauses] (:children expr)]
    (common/analyze-expression** ctx pattern)
    (doseq [[clause ret] (partition 2 clauses)]
      (analyze-clause ctx clause ret))))
