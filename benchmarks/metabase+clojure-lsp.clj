;; invoke with clj -M:profiler:bench benchmarks/metabase+clojure-lsp.clj
;; Allocation measurements require JDK21+. If you need to run on earlier JDK,
;; comment allocation-related parts.
(require '[clojure-lsp.api :as api]
         '[clojure.java.io :as io])

(import
 '[com.sun.management ThreadMXBean]
 '[java.lang.management ManagementFactory])

(def ^ThreadMXBean thread-bean (ManagementFactory/getThreadMXBean))

(def bytes-before (.getTotalThreadAllocatedBytes thread-bean))

(time (clojure-lsp.api/analyze-project-only! {:project-root (clojure.java.io/file "/Users/borkdude/dev/metabase")}))

(let [bytes-after (.getTotalThreadAllocatedBytes thread-bean)
      total-allocated (- bytes-after bytes-before)]
  (println (format "Total allocated: %.1fGB" (double (/ total-allocated 1e9)))))

(prn :done)
(shutdown-agents)
