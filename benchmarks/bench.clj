;; invoke with: clj -M:bench benchmarks/bench.clj <path> [iterations]
;; In-process timing of kondo/run! with warmup; reports all runs + min/median.
(require '[clj-kondo.core :as kondo])

(def path (or (first *command-line-args*) "src"))
(def iterations (parse-long (or (second *command-line-args*) "5")))

(def ^com.sun.management.ThreadMXBean thread-bean
  (java.lang.management.ManagementFactory/getThreadMXBean))

(defn run-once []
  (let [bytes-before (.getCurrentThreadAllocatedBytes thread-bean)
        start (System/nanoTime)
        result (kondo/run! {:lint [path] :cache false})
        elapsed-ms (/ (- (System/nanoTime) start) 1e6)
        allocated (- (.getCurrentThreadAllocatedBytes thread-bean) bytes-before)]
    {:ms elapsed-ms :findings (count (:findings result)) :mb (/ allocated 1e6)}))

(println "Warmup...")
(dotimes [_ 2] (run-once))

(let [runs (vec (repeatedly iterations run-once))
      times (mapv :ms runs)
      sorted (sort times)]
  (doseq [r runs]
    (println (format "run: %.1fms findings: %d alloc: %.1fMB" (:ms r) (:findings r) (:mb r))))
  (println (format "min: %.1fms median: %.1fms alloc-min: %.1fMB"
                   (first sorted)
                   (nth sorted (quot (count sorted) 2))
                   (apply min (map :mb runs)))))

(shutdown-agents)
