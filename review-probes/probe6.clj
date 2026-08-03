(ns probe6 (:require [clj-kondo.core :as clj-kondo]))
(defn f [s] (->> (with-in-str s (clj-kondo/run! {:lint ["-"] :cache false})) :findings (mapv (juxt :type :row :message))))
(defn -main [& _]
  (println "redefined-var in comment :" (f "(ns foo)\n(def x 1)\n(comment (def x 2))"))
  (println "redefined-var plain      :" (f "(ns foo)\n(def x 1)\n(def x 2)"))
  (shutdown-agents))
