(ns clj-kondo.tools.find-var
  (:require
   [clj-kondo.core :as clj-kondo]
   [clojure.string :as str]))

(defn -main [var & paths]
  (let [[var-ns var-name] (map symbol (str/split var #"/"))
        analysis (:analysis (clj-kondo/run! {:lint paths
                                             :config {:analysis true}}))
        {:keys [var-definitions var-usages]} analysis
        defined (keep (fn [{:keys [ns name] :as d}]
                        (when (and (= var-ns ns)
                                   (= var-name name))
                          d))
                      var-definitions)
        usages (keep (fn [{:keys [to name] :as d}]
                       (when (and (= var-ns to)
                                  (= var-name name))
                         d))
                     var-usages)]
    (doseq [{:keys [filename row col]} (sort-by (juxt :filename :row :col) defined)]
      (println (str var " is defined at " filename ":" row ":" col)))
    (doseq [{:keys [filename row col]} (sort-by (juxt :filename :row :col) usages)]
      (println (str var " is used at " filename ":" row ":" col)))))
