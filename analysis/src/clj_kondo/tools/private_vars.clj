(ns clj-kondo.tools.private-vars
  (:require [clojure.set :as set]
            [clj-kondo.core :as clj-kondo]))

(defn -main [& paths]
  (let [analysis (:analysis (clj-kondo/run! {:lint paths :config {:analysis true}}))
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
