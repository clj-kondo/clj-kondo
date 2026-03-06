;; invoke with clj -M:profiler:bench benchmarks/alloc-profile.clj
;; Opens flamegraph in browser when done
(require '[clj-async-profiler.core :as prof]
         '[clojure-lsp.api :as api]
         '[clojure.java.io :as io]
         '[clojure.string :as str])

(prof/profile
 {:event :alloc}
 (clojure-lsp.api/analyze-project-only! {:project-root (clojure.java.io/file "/Users/borkdude/dev/metabase")}))

(println "Flamegraph written to:" (prof/generate-flamegraph))

;; Copy collapsed stacks for programmatic analysis
(let [results-dir (clojure.java.io/file "/tmp/clj-async-profiler/results")
      txt-files (->> (.listFiles results-dir)
                     (filter #(str/ends-with? (.getName %) ".txt"))
                     (sort-by #(.lastModified %))
                     reverse)
      latest (first txt-files)]
  (when latest
    (clojure.java.io/copy latest (clojure.java.io/file "/tmp/alloc-collapsed.txt"))
    (println "Collapsed stacks copied to /tmp/alloc-collapsed.txt")))

(shutdown-agents)
