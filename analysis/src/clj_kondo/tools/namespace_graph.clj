(ns clj-kondo.tools.namespace-graph
  (:require [clj-kondo.core :as clj-kondo]
            [loom.graph :refer [digraph]]
            [loom.io :refer [view]]))

(defn -main [& paths]
  (let [analysis (:analysis (clj-kondo/run! {:lint paths
                                             :config {:analysis {:var-usages false
                                                                 :var-definitions {:shallow true}}}
                                             :skip-lint true}))
        {:keys [:namespace-definitions :namespace-usages]} analysis
        nodes (map :name namespace-definitions)
        edges (map (juxt :from :to) namespace-usages)
        g (apply digraph (concat nodes edges))]
    ;; install GraphViz, e.g. with brew install graphviz
    (view g)))
