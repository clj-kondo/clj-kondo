(ns clj-kondo.unused-vars
  (:require [clj-kondo.core :as clj-kondo]
            [clojure.set :as set]
            [clojure.string :as str]))

(defn -main [& [lint-path]]
  (let [analysis (:analysis (clj-kondo/run! {:lint [(or lint-path "../src")] ;; clj-kondo itself
                                             :config {:output {:analysis true}}}))
        {:keys [:var-definitions :var-usages]} analysis
        defined-vars (set (map (juxt :ns :name) var-definitions))
        used-vars (set (map (juxt :to :name) var-usages))
        unused-vars (map (fn [[ns v]]
                           (symbol (str ns) (str v)))
                         (set/difference defined-vars used-vars))]
    (println "The following vars are unused:")
    (println (str/join "\n" unused-vars))))
