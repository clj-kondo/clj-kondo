(ns clj-kondo.impl.analyzer.clojure-data-xml
  (:require [clj-kondo.impl.analyzer.common :as common]
            [clj-kondo.impl.utils :as utils :refer [list-node generated-token]]))

(set! *warn-on-reflection* true)

(defn ->alias-node [alias-node]
  (let [sexpr (utils/sexpr alias-node)]
    (cond (string? sexpr)
          (list-node [(generated-token 'quote)
                      (generated-token (symbol sexpr))])
          (and (list? sexpr)
               (= 'quote (first sexpr)))
          (list-node [(generated-token 'quote)
                      (generated-token (symbol (str (second sexpr))))])
          (keyword? sexpr)
          (list-node [(generated-token 'quote)
                      (generated-token (symbol (name sexpr)))])
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
                                        [(generated-token 'alias)
                                         (->alias-node alias-node)
                                         (list-node [(generated-token 'quote)
                                                     (generated-token uri)])]))
          (recur (nnext children)))))))

(defn analyze-export-api [ctx node]
  (let [children (rest (:children node))
        new-node (utils/list-node
                  (list* (generated-token 'do)
                         (map (fn [exported-var-node]
                                (let [qualified-sym (:value exported-var-node)
                                      unqualified-sym (symbol (name qualified-sym))]
                                  (utils/list-node [(generated-token 'clojure.core/def)
                                                    (generated-token unqualified-sym)
                                                    exported-var-node])))
                              children)))]
    (common/analyze-expression** ctx new-node)))
