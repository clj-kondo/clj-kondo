(ns clj-kondo.impl.analyzer.clojure-data-xml
  (:require [clj-kondo.impl.analyzer.common :as common]
            [clj-kondo.impl.utils :as utils :refer [list-node token-node]]))

(set! *warn-on-reflection* true)

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

(defn encode-uri [^String uri]
  (java.net.URLEncoder/encode uri "UTF-8"))

(defn uri-symbol [uri]
  (symbol (encode-uri (str "xmlns." uri))))

(defn analyze-alias-uri [ctx expr]
  (let [children (next (:children expr))]
    (common/analyze-children ctx children)
    (loop [children children]
      (when children
        (let [alias-node (first children)
              ns-str-node (second children)
              ns-str (utils/sexpr ns-str-node)
              uri (uri-symbol ns-str)]
          (common/analyze-expression** ctx
                                       (list-node
                                        [(token-node 'alias)
                                         (->alias-node alias-node)
                                         (list-node [(token-node 'quote)
                                                     (token-node uri)])]))
          (recur (nnext children)))))))
