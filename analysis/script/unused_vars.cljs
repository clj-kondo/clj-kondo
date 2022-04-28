#!/usr/bin/env plk -K

(ns script.unused-vars
  (:require [cljs.reader :as edn]
            [clojure.set :as set]
            [clojure.string :as str]
            [planck.shell :refer [sh]]))

(defn -main [& paths]
  (let [out (:out (apply sh "clj-kondo" "--config" "{:output {:format :edn}, :analysis true}"
                         "--lint" paths))
        analysis (:analysis (edn/read-string out))
        {:keys [:var-definitions :var-usages]} analysis
        defined-vars (set (map (juxt :ns :name) var-definitions))
        used-vars (set (map (juxt :to :name) var-usages))
        unused-vars (map (fn [[ns v]]
                           (symbol (str ns) (str v)))
                         (set/difference defined-vars used-vars))]
    (if (seq unused-vars)
      (do (println "The following vars are unused:")
          (println (str/join "\n" unused-vars)))
      (println "No unused vars found."))))

(set! *main-cli-fn* -main)
