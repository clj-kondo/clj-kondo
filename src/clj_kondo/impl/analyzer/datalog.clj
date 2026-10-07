(ns clj-kondo.impl.analyzer.datalog
  {:no-doc true}
  (:require
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.utils :as utils :refer [node->line one-of sexpr tag]]
   [datalog.parser :as datalog]))

(set! *warn-on-reflection* true)

(def ^:private engines-with-implicit-rules
  "Engines that pre-install rules, so a rule expression in a query of theirs
  does not need a `%` binding in `:in`. Datahike seeds every query context with
  its bitemporal rules (`valid-at` and friends) and rewrites a query calling one
  to pass them along."
  '#{datahike.api})

(defn- implicit-rules?
  "Config `:implicit-rules` overrides the per-engine default of `:auto`."
  [ctx engine]
  (let [configured (get-in (:config ctx) [:linters :datalog-syntax :implicit-rules] :auto)]
    (if (identical? :auto configured)
      (contains? engines-with-implicit-rules engine)
      (boolean configured))))

(defn analyze-datalog [ctx expr engine]
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
        (datalog/parse (sexpr datalog-node)
                       {:implicit-rules? (implicit-rules? ctx engine)})
        nil
        (catch Exception e
          (findings/reg-finding! ctx
                                 (node->line (:filename ctx) query-raw
                                             :datalog-syntax
                                             (.getMessage e))))))))
