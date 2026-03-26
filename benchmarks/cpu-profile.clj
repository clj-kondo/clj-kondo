;; invoke with clj -M:profiler:bench benchmarks/cpu-profile.clj
(require '[clj-async-profiler.core :as prof]
         '[clojure-lsp.api :as api]
         '[clojure.java.io :as io])

(prof/profile
 {:event :cpu}
 (clojure-lsp.api/analyze-project-only! {:project-root (clojure.java.io/file "/Users/borkdude/dev/metabase")}))

(println "Done. Flamegraph in /tmp/clj-async-profiler/results/")
(shutdown-agents)
