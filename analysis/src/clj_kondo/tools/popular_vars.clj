(ns clj-kondo.tools.popular-vars
  (:require [clj-kondo.core :as clj-kondo]))

(defn -main [n & paths]
  (let [n (Integer. n)
        analysis (:analysis (clj-kondo/run! {:lint paths
                                             :config {:analysis true}}))
        {:keys [:var-usages]} analysis
        vars (map (juxt :to :name) var-usages)
        freqs (frequencies vars)
        freqs (sort-by second > freqs)
        top-n (take n freqs)]
    (doseq [[[ns name] n] top-n]
      (println (str ns "/" name ": " n)))))
