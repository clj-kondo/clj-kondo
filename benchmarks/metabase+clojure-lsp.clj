;; invoke with clj -M:profiler:bench benchmarks/metabase+clojure-lsp.clj
(require '[clojure-lsp.api :as api]
         '[clojure.java.io :as io])

(time (clojure-lsp.api/analyze-project-only! {:project-root (clojure.java.io/file "/Users/borkdude/dev/metabase")}))
(prn :done)
(shutdown-agents)
