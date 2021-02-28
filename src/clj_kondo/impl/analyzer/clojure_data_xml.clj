(ns clj-kondo.impl.analyzer.clojure-data-xml
  (:require [clj-kondo.impl.analyzer.common :as common]
            [clj-kondo.impl.utils :as utils :refer [list-node token-node]]))

(defn analyze-alias-uri [ctx expr]
  (let [children (next (:children expr))
        alias-node (first children)
        ns-str-node (second children)]
    (common/analyze-children ctx children)
    (common/analyze-expression** ctx
                                 (list-node
                                  [(token-node 'alias)
                                   alias-node
                                   (list-node [(token-node 'quote)
                                               (token-node (symbol (str (utils/sexpr ns-str-node))))])]))))
