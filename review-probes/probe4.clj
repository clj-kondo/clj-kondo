(ns probe4
  (:require [babashka.fs :as fs] [clj-kondo.core :as clj-kondo]))
(defn -main [& _]
  (fs/with-temp-dir [tmp {}]
    (let [proj (fs/file tmp "proj") cache (str (fs/file tmp ".cache"))]
      (fs/create-dirs proj)
      (dotimes [i 3]
        (spit (fs/file proj (str "n" i ".clj"))
              (str "(ns n" i " (:require [clojure.spec.alpha :as s]))\n(s/def ::x string?)")))
      (println "-- lint dir:")
      (clj-kondo/run! {:lint [(str proj)] :cache-dir cache})
      (println "cache contents:" (mapv str (fs/list-dir cache)))
      (println "-- lint single file:")
      (clj-kondo/run! {:lint [(str (fs/file proj "n0.clj"))] :cache-dir cache})
      (println "cache contents:" (mapv str (fs/list-dir cache)))))
  (shutdown-agents))
