(ns clj-kondo.namespace-graph
  (:require [clj-kondo.core :as clj-kondo]
            [loom.graph :refer [graph]]
            [loom.io :refer [view]]))

(defn -main [& [lint-path]]
  (let [analysis (:analysis (clj-kondo/run! {:lint [(or lint-path "../src")] ;; clj-kondo itself
                                             :config {:output {:analysis true}}}))
        {:keys [:namespace-definitions :namespace-usages]} analysis
        nodes (map :name namespace-definitions)
        edges (map (juxt :from :to) namespace-usages)
        g (apply graph (concat nodes edges ))]
    ;; install GraphViz, e.g. with brew install graphviz
    (view g)))
