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

  - `:filename`: optional. In case stdin is used for linting, use this
  to set the reported filename.

  - `:cache-dir`: when this option is provided, the cache will be
  resolved to this directory. If `:cache` is `false` this option will
  be ignored.

  - `:cache`: if `false`, won't use cache. Otherwise, will try to resolve cache
  using `:cache-dir`. If `:cache-dir` is not set, cache is resolved using the
  nearest `.clj-kondo` directory in the current and parent directories.

  - `:config`: optional. A seqable of maps, a map or string
  representing the config as EDN, or a config file.

  In places where a file-like value is expected, either a path as string or a
  `java.io.File` may be passed, except for a classpath which must always be a string.

  - `:parallel`: optional. A boolean indicating if sources should be linted in parallel.`

  Returns a map with `:findings`, a seqable of finding maps, a
  `:summary` of the findings and the `:config` that was used to
  produce those findings. This map can be passed to `print!` to print
  to `*out*`. Alpha, subject to change.
  "
  [{:keys [:lint
           :lang
           :filename
           :cache
           :cache-dir
           :config
           :config-dir
           :parallel
           :no-warnings]
    :or {cache true}}]
  (let [start-time (System/currentTimeMillis)
        cfg-dir
        (cond config-dir (io/file config-dir)
              filename (core-impl/config-dir filename)
              :else
              (core-impl/config-dir (io/file (System/getProperty "user.dir"))))
        ;; for backward compatibility non-sequential config should be wrapped into collection
        config (core-impl/resolve-config cfg-dir (if (sequential? config) config [config]))
        classpath (:classpath config)
        config (dissoc config :classpath)
        cache-dir (when cache (core-impl/resolve-cache-dir cfg-dir cache cache-dir))
        files (atom 0)
        findings (atom [])
        analysis-cfg (get-in config [:output :analysis])
        analyze-locals? (get analysis-cfg :locals)
        analyze-keywords? (get analysis-cfg :keywords)
        analysis (when analysis-cfg
                   (atom (cond-> {:namespace-definitions []
                                  :namespace-usages []
                                  :var-definitions []
                                  :var-usages []}
                           analyze-locals? (assoc :locals []
                                                  :local-usages [])
                           analyze-keywords? (assoc :keywords []))))
        used-nss (atom {:clj #{}
                        :cljs #{}
                        :cljc #{}})
        ctx {:no-warnings no-warnings
             :config-dir cfg-dir
             :config config
             :classpath classpath
             :global-config config
             :sources (atom [])
             :files files
             :findings findings
             :namespaces (atom {})
             :analysis analysis
             :cache-dir cache-dir
             :used-namespaces used-nss
             :ignores (atom {})
             :id-gen (when analyze-locals? (atom 0))
             :analyze-locals? analyze-locals?
             :analyze-keywords? analyze-keywords?
             :analyze-arglists? (get analysis-cfg :arglists)}
        lang (or lang :clj)
        _ (core-impl/process-files (if parallel
                                     (assoc ctx :parallel parallel)
                                     ctx) lint lang filename)
        ;; _ (prn :used-nss @used-nss)
        idacs (core-impl/index-defs-and-calls ctx)
        idacs (cache/sync-cache idacs cache-dir)
        idacs (overrides idacs)
        _ (when (and no-warnings (not analysis))
            ;; analysis is called from lint-var-usage, this can probably happen somewhere else
            (l/lint-var-usage ctx idacs))
        _ (when-not no-warnings
            (l/lint-var-usage ctx idacs)
            (l/lint-unused-namespaces! ctx)
            (l/lint-unused-private-vars! ctx)
            (l/lint-unused-bindings! ctx)
            (l/lint-unresolved-symbols! ctx)
            (l/lint-unresolved-vars! ctx)
            (l/lint-unused-imports! ctx)
            (l/lint-unresolved-namespaces! ctx))
        all-findings @findings
        all-findings (core-impl/filter-findings config all-findings)
        all-findings (into [] (dedupe) (sort-by (juxt :filename :row :col) all-findings))
        summary (core-impl/summarize all-findings)
        duration (- (System/currentTimeMillis) start-time)
        summary (assoc summary :duration duration :files @files)]
    (cond->
        {:findings all-findings
         :config config
         :summary summary}
      analysis-cfg
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

  (-> (run!
       {;; seq of string or file
        :files ["corpus" (io/file "test")]
        :config [{:linters {:invalid-arity {:level :off}}}
                 {:linters {:invalid-arity {:level :warning}}}]
        ;; :cache takes a string, file or boolean
        :cache (io/file "/tmp/clj-kondo-cache")
        ;; only relevant when linting stdin
        :lang :clj})
      :config
      :linters
      :invalid-arity)

  )
