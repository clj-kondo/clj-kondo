(ns clj-kondo.core
  (:refer-clojure :exclude [run!])
  (:require
   [cheshire.core :as cheshire]
   [clj-kondo.impl.cache :as cache]
   [clj-kondo.impl.config :refer [merge-config!]]
   [clj-kondo.impl.core :as core-impl]
   [clj-kondo.impl.linters :as l]
   [clj-kondo.impl.overrides :refer [overrides]]
   [clojure.java.io :as io]))

;;;; Public API

(defn print!
  "Prints the result from `run!` to `*out*`. Returns `nil`. Alpha,
  subject to change."
  [{:keys [:config :findings :summary :analysis]}]
  (let [output-cfg (:output config)
        fmt (or (:format output-cfg) :text)]
    (case fmt
      :text
      (do
        (when (:progress output-cfg) (println))
        (let [format-fn (core-impl/format-output config)]
          (doseq [{:keys [:filename :message
                          :level :row :col] :as _finding}
                  findings]
            (println (format-fn filename row col level message)))
          (when (:summary output-cfg)
            (let [{:keys [:error :warning :duration]} summary]
              (printf "linting took %sms, " duration)
              (println (format "errors: %s, warnings: %s" error warning))))))
      ;; avoid loading clojure.pprint or bringing in additional libs for printing to EDN for now
      :edn
      (let [output (cond-> {:findings findings}
                     (:summary output-cfg)
                     (assoc :summary summary)
                     (:analysis output-cfg)
                     (assoc :analysis analysis))]
        (prn output))
      :json
      (println (cheshire/generate-string
                (cond-> {:findings findings}
                  (:summary output-cfg)
                  (assoc :summary summary)
                  (:analysis output-cfg)
                  (assoc :analysis analysis))))))
  (flush)
  nil)

(defn run!
  "Takes a map with:

  - `:lint`: a seqable of files, directories and/or classpaths to lint.

  - `:lang`: optional, defaults to `:clj`. Sets language for linting
  `*in*`. Supported values: `:clj`, `:cljs` and `:cljc`.

  - `:cache-dir`: when this option is provided, the cache will be
  resolved to this directory. If `:cache` is `false` this option will
  be ignored.

  - `:cache`: if `false`, won't use cache. Otherwise, will try to resolve cache
  using `:cache-dir`. If `:cache-dir` is not set, cache is resolved using the
  nearest `.clj-kondo` directory in the current and parent directories.

  - `:config`: optional. Map or string representing the config as EDN,
  or a config file.

  In places where a file-like value is expected, either a path as string or a
  `java.io.File` may be passed, except for a classpath which must always be a string.

  Returns a map with `:findings`, a seqable of finding maps, a
  `:summary` of the findings and the `:config` that was used to
  produce those findings. This map can be passed to `print!` to print
  to `*out*`. Alpha, subject to change.
  "
  [{:keys [:lint
           :lang
           :cache
           :cache-dir
           :config]
    :or {cache true}}]
  (let [start-time (System/currentTimeMillis)
        cfg-dir (core-impl/config-dir (io/file (System/getProperty "user.dir")))
        config (core-impl/resolve-config cfg-dir config)
        cache-dir (when cache (core-impl/resolve-cache-dir cfg-dir cache cache-dir))
        findings (atom [])
        analysis? (get-in config [:output :analysis])
        analysis (when analysis?
                   (atom {:namespace-definitions []
                          :namespace-usages []
                          :var-definitions []
                          :var-usages []}))
        ctx {:config config
             :findings findings
             :namespaces (atom {})
             :analysis analysis
             :cache-dir cache-dir}
        lang (or lang :clj)
        processed
        ;; this is needed to force the namespace atom state
        (doall (core-impl/process-files ctx lint lang))
        idacs (core-impl/index-defs-and-calls ctx processed)
        idacs (cache/sync-cache idacs cache-dir)
        idacs (overrides idacs)
        _ (l/lint-var-usage ctx idacs)
        _ (l/lint-unused-namespaces! ctx)
        _ (l/lint-unused-private-vars! ctx)
        _ (l/lint-unused-bindings! ctx)
        _ (l/lint-unused-destructuring-defaults! ctx)
        _ (l/lint-unresolved-symbols! ctx)
        _ (l/lint-unused-imports! ctx)
        _ (l/lint-unresolved-namespaces! ctx)
        ;; _ (namespace/reg-analysis-output! ctx)
        all-findings @findings
        all-findings (core-impl/filter-findings config all-findings)
        all-findings (into [] (dedupe) (sort-by (juxt :filename :row :col) all-findings))
        summary (core-impl/summarize all-findings)
        duration (- (System/currentTimeMillis) start-time)
        summary (assoc summary :duration duration)]
    (cond->
        {:findings all-findings
         :config config
         :summary summary}
      analysis?
      (assoc :analysis @analysis))))

(defn merge-configs
  "Returns the merged configuration of c1 with c2."
  ([& configs]
   (reduce merge-config! configs)))

;;;; Scratch

(comment
  (def res (run!
            {;; seq of string or file
             :files ["corpus" (io/file "test")]
             :config {:linters {:invalid-arity {:level :off}}}
             ;; :cache takes a string, file or boolean
             :cache (io/file "/tmp/clj-kondo-cache")
             ;; only relevant when linting stdin
             :lang :clj}))
  (first (:findings res))
  (print! res)
  )
