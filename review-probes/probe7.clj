(ns probe7 (:require [babashka.fs :as fs] [clj-kondo.core :as clj-kondo]))
(def req "[clojure.spec.alpha :as s]")
(defn redef [r] (->> r :findings (filter #(= :redefined-spec (:type %))) (mapv :message)))
(defn -main [& _]
  (fs/with-temp-dir [tmp {}]
    (let [cache (str (fs/file tmp ".cache"))
          abs (str (fs/file tmp "a.clj"))
          rel (str (fs/relativize (fs/cwd) abs))]
      (spit abs (str "(ns a (:require " req "))\n(s/def ::x string?)"))
      (println "cwd:" (str (fs/cwd)))
      (println "run 1 (relative path) ->" (redef (clj-kondo/run! {:lint [rel] :cache-dir cache})))
      (println "run 2 (absolute path) ->" (redef (clj-kondo/run! {:lint [abs] :cache-dir cache})))
      (println "run 3 (relative again)->" (redef (clj-kondo/run! {:lint [rel] :cache-dir cache})))))
  (shutdown-agents))
