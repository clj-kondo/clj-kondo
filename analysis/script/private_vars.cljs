#!/usr/bin/env plk -K

(ns script.private-vars
  (:require [cljs.reader :as edn]
            [planck.shell :refer [sh]]
            [clojure.set :as set]))

(defn -main [& paths]
  (let [out (:out (apply sh "clj-kondo" "--config" "{:output {:format :edn}, :analysis true}"
                         "--lint" paths))
        analysis (:analysis (edn/read-string out))
        {:keys [:var-definitions :var-usages]} analysis
        private-var-defs (->> var-definitions
                               (filter :private)
                               (group-by (juxt :ns :name)))
        private-var-usages (->> var-usages
                                (filter (fn [{:keys [:to :name]}]
                                          (contains? private-var-defs [to name])))
                                (group-by (juxt :to :name)))
        unused-keys (set/difference (set (keys private-var-defs))
                                    (set (keys private-var-usages)))
        unused (->> (select-keys private-var-defs unused-keys)
                    vals
                    (map first))
        illegal (keep (fn [{:keys [:from :to] :as v}]
                        (when (not= from to)
                          v))
                      (apply concat (vals private-var-usages)))]
    (doseq [{:keys [:ns :name :filename :row :col]} unused]
      (println (str filename ":" row ":" col " warning: " ns "/" name " is private but never used")))
    (doseq [{:keys [:from :to :name :filename :row :col]} illegal]
      (println (str filename ":" row ":" col " warning: " to "/" name
                    " is private and cannot be accessed from namespace "
                    from)))))

(set! *main-cli-fn* -main)
