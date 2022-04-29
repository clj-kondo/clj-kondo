(ns clj-kondo.tools.circular-dependencies
  (:require [clj-kondo.core :as clj-kondo]
            [com.stuartsierra.dependency :as dep]))

(defn -main [& paths]
  (let [analysis (:analysis (clj-kondo/run! {:lint paths
                                             :config {:analysis {:var-usages false
                                                                 :var-definitions {:shallow true}}}
                                             :skip-lint true}))
        {:keys [:namespace-usages]} analysis]
    (reduce (fn [graph {:keys [:from :to :filename :row :col]}]
              (try (dep/depend graph from to)
                   (catch Exception e
                     (let [ed (ex-data e)]
                       (if (= :com.stuartsierra.dependency/circular-dependency
                              (:reason ed))
                         (do (println (str filename ":" row ":" col ":")
                                      "circular dependency from namespace"
                                      from "to" to)
                             graph)
                         (throw e))))))
            (dep/graph)
            namespace-usages)))
