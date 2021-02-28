(ns clj-kondo.impl.analyzer.clojure-data-xml
  (:require [clj-kondo.impl.analyzer.common :as common]
            [clj-kondo.impl.utils :as utils :refer [list-node token-node]]))

(defn ->alias-node [alias-node]
  (let [sexpr (utils/sexpr alias-node)]
    (cond (string? sexpr)
          (list-node [(token-node 'quote)
                      (token-node (symbol sexpr))])
          (and (list? sexpr)
               (= 'quote (first sexpr)))
          (list-node [(token-node 'quote)
                      (token-node (symbol (str (second sexpr))))])
          (keyword? sexpr)
          (list-node [(token-node 'quote)
                      (token-node (symbol (name sexpr)))])
          :else alias-node)))

(defn analyze-alias-uri [ctx expr]
  (let [children (next (:children expr))]
    (common/analyze-children ctx children)
    (loop [children children]
      (when children
        (let [alias-node (first children)
              ns-str-node (second children)]
          (common/analyze-expression** ctx
                                       (list-node
                                        [(token-node 'alias)
                                         (->alias-node alias-node)
                                         (list-node [(token-node 'quote)
                                                     (token-node (symbol (str (utils/sexpr ns-str-node))))])]))
          (recur (nnext children)))))))
