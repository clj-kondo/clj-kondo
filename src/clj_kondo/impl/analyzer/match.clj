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

(defn analyze-clause [ctx clause]
  (case (utils/tag clause)
    :vector
    (vector-bindings ctx clause)
    :list
    (vector-bindings ctx clause)
    ;; TODO: map
    ;; fallback
    (common/analyze-expression** ctx clause)
    ))

(defn analyze-match [ctx expr]
  (let [[_match pattern & clauses] (:children expr)]
    (common/analyze-expression** ctx pattern)
    (doseq [[clause ret] (partition 2 clauses)]
      (let [bindings (analyze-clause ctx clause)
            ctx (if bindings
                  (utils/ctx-with-bindings ctx bindings)
                  ctx)]
        (common/analyze-expression** ctx ret)))))
