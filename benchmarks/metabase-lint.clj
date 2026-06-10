;; invoke with:
;; cd /path/to/metabase && clj -Sdeps '{:deps {clj-kondo/clj-kondo {:local/root "/Users/borkdude/dev/clj-kondo"}}}' benchmarks/metabase-lint.clj
;; Allocation measurements require JDK21+. If you need to run on earlier JDK,
(require '[clj-kondo.core :as kondo]
         '[clojure.java.io :as io])

(import
 '[com.sun.management ThreadMXBean]
 '[java.lang.management ManagementFactory])

(def ^ThreadMXBean thread-bean (ManagementFactory/getThreadMXBean))

(def bytes-before (.getTotalThreadAllocatedBytes thread-bean))

(let [start (System/currentTimeMillis)
      result (kondo/run! {:lint ["src"]})
      elapsed (- (System/currentTimeMillis) start)
      bytes-after (.getTotalThreadAllocatedBytes thread-bean)
      total-allocated (- bytes-after bytes-before)]
  (println (format "Linting took %dms, errors: %d, warnings: %d"
                   elapsed
                   (count (filter #(= :error (:level %)) (:findings result)))
                   (count (filter #(= :warning (:level %)) (:findings result)))))
  (println (format "Total allocated: %.1fGB" (double (/ total-allocated 1e9)))))

(shutdown-agents)
