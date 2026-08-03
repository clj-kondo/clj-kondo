(ns probe3
  "Scaling of the global spec index in the editor loop: lint one file over and
  over against a project-sized index."
  (:require [babashka.fs :as fs]
            [clj-kondo.core :as clj-kondo]
            [clojure.string :as str]))

(defn ms [f] (let [t (System/nanoTime) v (f)] [(/ (- (System/nanoTime) t) 1e6) v]))

(defn gen-file [dir i n-specs]
  (let [f (fs/file dir (str "ns" i ".clj"))]
    (spit f (str "(ns ns" i " (:require [clojure.spec.alpha :as s]))\n"
                 (str/join "\n" (for [j (range n-specs)]
                                  (str "(s/def ::spec" j " string?)")))))
    (str f)))

(defn -main [& [n-files n-specs]]
  (let [n-files (parse-long (or n-files "500"))
        n-specs (parse-long (or n-specs "20"))]
    (fs/with-temp-dir [tmp {}]
      (let [cache (str (fs/file tmp ".cache"))
            files (mapv #(gen-file tmp % n-specs) (range n-files))
            idx (delay (first (fs/glob cache "**spec-index.transit.json")))]
        (println (format "%d files x %d specs = %d registrations"
                         n-files n-specs (* n-files n-specs)))
        (let [[t r] (ms (fn [] (clj-kondo/run! {:lint [(str tmp)] :cache-dir cache})))]
          (println (format "full project lint     : %8.1f ms (findings %d)" t (count (:findings r)))))
        (println "spec index on disk    :" (when @idx (fs/size @idx)) "bytes")
        (dotimes [i 3]
          (let [[t _] (ms (fn [] (clj-kondo/run! {:lint [(first files)] :cache-dir cache})))]
            (println (format "single-file re-lint %d : %8.1f ms  (linter on)" i t))))
        (dotimes [i 3]
          (let [[t _] (ms (fn [] (clj-kondo/run! {:lint [(first files)] :cache-dir cache
                                                  :config {:linters {:redefined-spec {:level :off}}}})))]
            (println (format "single-file re-lint %d : %8.1f ms  (linter off)" i t))))
        ;; and the full-project case with the index already populated
        (dotimes [i 2]
          (let [[t _] (ms (fn [] (clj-kondo/run! {:lint [(str tmp)] :cache-dir cache})))]
            (println (format "full project re-lint %d: %8.1f ms  (linter on)" i t))))
        (dotimes [i 2]
          (let [[t _] (ms (fn [] (clj-kondo/run! {:lint [(str tmp)] :cache-dir cache
                                                  :config {:linters {:redefined-spec {:level :off}}}})))]
            (println (format "full project re-lint %d: %8.1f ms  (linter off)" i t))))))
    (shutdown-agents)))
