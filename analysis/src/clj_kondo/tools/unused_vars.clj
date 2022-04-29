(ns clj-kondo.tools.unused-vars
  (:require [clj-kondo.core :as clj-kondo]
            [clojure.set :as set]
            [clojure.string :as str]))

(defn -main [& paths]
  (if (empty? paths)
    (println "Provide paths for analysis.")
    (let [analysis (:analysis (clj-kondo/run! {:lint paths
                                               :config {:analysis true}}))
          {:keys [:var-definitions :var-usages]} analysis
          defined-vars (set (map (juxt :ns :name) var-definitions))
          used-vars (set (map (juxt :to :name) var-usages))
          unused-vars (map (fn [[ns v]]
                             (symbol (str ns) (str v)))
                           (set/difference defined-vars used-vars))]
      (if (seq unused-vars)
        (do (println "The following vars are unused:")
            (println (str/join "\n" unused-vars)))
        (println "No unused vars found.")))))
