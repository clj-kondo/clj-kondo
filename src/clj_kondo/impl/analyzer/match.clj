(ns clj-kondo.impl.analyzer.match
  (:require [clj-kondo.impl.analyzer.common :as common]
            [clj-kondo.impl.utils :as utils]))

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
                   (into bindings bnds))
            (recur (next children) bindings)))
        bindings))))

(defn analyze-list [ctx expr]
  (let [children (:children expr)
        snd (second children)]
    (if-let [k (:k snd)]
      (if (or (identical? :<< k)
              (identical? :when k))
        ;; https://github.com/clojure/core.match/blob/fb3188934ab9b6df0249ba3092a888def3434eee/src/main/clojure/clojure/core/match.clj#L1835
        (let [bnds (analyze-expr ctx (first children))]
          (common/analyze-children ctx {:children (nnext children)})
          bnds)
        (analyze-children ctx expr))
      (analyze-children ctx expr))))

(defn analyze-expr [ctx expr]
  (let [tag (utils/tag expr)]
    (case tag
      :token
      (analyze-token ctx expr)
      (:list)
      (analyze-list ctx expr)
      (:vector :map)
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

;; TODO:

;; defpred
;; (defpred even?)
;; (defpred odd?)
;; (defpred div3?)

;; (deftest guard-pattern-match-1
;;   (is (= (let [y '(2 3 4 5)]
;;            (match [y]
;;                   [([_ (a :when even?) _ _] :seq)] :a0
;;                   [([_ (b :when [odd? div3?]) _ _] :seq)] :a1
;;                   :else []))
;;          :a1)))
