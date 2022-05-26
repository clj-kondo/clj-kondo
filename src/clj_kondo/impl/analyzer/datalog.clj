(ns clj-kondo.impl.analyzer.datalog
  {:no-doc true}
  (:require
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.utils :as utils :refer
    [node->line tag one-of tag sexpr]]
   [datalog.parser :as datalog]))

(set! *warn-on-reflection* true)

(defn analyze-datalog [ctx expr]
  (let [children (next (:children expr))
        query-raw (first children)
        quoted? (when query-raw
                  (= :quote (tag query-raw)))
        datalog-node (when quoted?
                       (when-let [edn-node (first (:children query-raw))]
                         (when (one-of (tag edn-node) [:vector :map])
                           edn-node)))]
    (when datalog-node
      (try
        (datalog/parse (sexpr datalog-node))
        nil
        (catch Exception e
          (findings/reg-finding! ctx
                                 (node->line (:filename ctx) query-raw
                                             :datalog-syntax
                                             (.getMessage e))))))))
