(ns probe2
  (:require [babashka.fs :as fs]
            [clj-kondo.core :as clj-kondo]))

(def req "[clojure.spec.alpha :as s]")

(defn redef [res]
  (->> res :findings (filter #(= :redefined-spec (:type %))) (mapv :message)))

(defn ms [f] (let [t (System/nanoTime) v (f)] [(/ (- (System/nanoTime) t) 1e6) v]))

(defn probe-path-spelling []
  (println "\n===== path spelling: same file, different path strings")
  (fs/with-temp-dir [tmp {}]
    (let [cache (str (fs/file tmp ".cache"))
          f (str (fs/file tmp "a.clj"))
          ;; a different string denoting the exact same file
          indirect (str tmp "/../" (fs/file-name tmp) "/a.clj")]
      (spit f (str "(ns a (:require " req "))\n(s/def ::x string?)"))
      (println "run 1, path =" f)
      (println "  ->" (redef (clj-kondo/run! {:lint [f] :cache-dir cache})))
      (println "run 2, path =" indirect)
      (println "  ->" (redef (clj-kondo/run! {:lint [indirect] :cache-dir cache})))
      (println "run 3, canonical-paths true, path =" indirect)
      (println "  ->" (redef (clj-kondo/run! {:lint [indirect] :cache-dir cache
                                              :config {:output {:canonical-paths true}}}))))))

(defn probe-metabase-cost [dirs]
  (println "\n===== cost on metabase (src + enterprise/backend)")
  (fs/with-temp-dir [tmp {}]
    (let [cache (str (fs/file tmp ".cache"))
          idx (fs/file cache "spec-index.transit.json")
          run (fn [cfg] (clj-kondo/run! {:lint dirs :cache-dir cache :config cfg}))]
      ;; warm the JIT and the index
      (run {})
      (doseq [i (range 2)]
        (let [[t r] (ms (fn [] (run {:linters {:redefined-spec {:level :off}}})))]
          (println (format "linter :off      run %d: %8.1f ms (findings %d)" i t (count (:findings r)))))
        (let [[t r] (ms (fn [] (run {})))]
          (println (format "linter :warning  run %d: %8.1f ms (findings %d, redefined-spec %d)"
                           i t (count (:findings r)) (count (redef r))))))
      (println "spec index size:" (when (fs/exists? idx) (fs/size idx)) "bytes")
      ;; single-file re-lint, the editor case, against the full project index
      (let [one (first (fs/glob (first dirs) "**.clj"))]
        (dotimes [i 3]
          (let [[t _] (ms (fn [] (clj-kondo/run! {:lint [(str one)] :cache-dir cache})))]
            (println (format "single-file re-lint run %d: %8.1f ms" i t))))
        (dotimes [i 3]
          (let [[t _] (ms (fn [] (clj-kondo/run! {:lint [(str one)] :cache-dir cache
                                                  :config {:linters {:redefined-spec {:level :off}}}})))]
            (println (format "single-file, linter off  %d: %8.1f ms" i t))))))))

(defn -main [& dirs]
  (probe-path-spelling)
  (probe-metabase-cost (vec dirs))
  (shutdown-agents))
