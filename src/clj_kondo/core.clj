(ns clj-kondo.core
  (:refer-clojure :exclude [run!])
  (:require
   [aaaa-this-has-to-be-first.because-patches]
   [cheshire.core :as cheshire]
   [clj-kondo.impl.cache :as cache]
   [clj-kondo.impl.config :refer [merge-config!]]
   [clj-kondo.impl.core :as core-impl]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.linters :as l]
   [clj-kondo.impl.overrides :refer [overrides]]
   [clj-kondo.impl.utils :as utils]
   [clojure.java.io :as io]))

(set! *warn-on-reflection* true)

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
        (when (:progress output-cfg) (binding [*out* *err*]
                                       (println)))
        (let [format-fn (core-impl/format-output config)]
          (doseq [finding findings]
            (println (format-fn finding)))
          (when (:summary output-cfg)
            (let [{:keys [:error :warning :duration]} summary]
              (printf "linting took %sms, " duration)
              (println (format "errors: %s, warnings: %s" error warning))))))
      ;; avoid loading clojure.pprint or bringing in additional libs for printing to EDN for now
      :edn
      (let [output (cond-> {:findings findings}
                     (:summary output-cfg)
                     (assoc :summary summary)
                     analysis
                     (assoc :analysis analysis))]
        (prn output))
      :json
      (println (cheshire/generate-string
                (cond-> {:findings findings}
                  (:summary output-cfg)
                  (assoc :summary summary)
                  analysis
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

  - `:copy-configs`: optional. A boolean indicating if scanned hooks should be copied to clj-kondo config dir.`

  - `:skip-lint`: optional. A boolean indicating if linting should be
  skipped. Other tasks like copying configs will still be done if `:copy-configs` is true.`

  - `:debug`: optional. Print debug info.

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
           :no-warnings
           :dependencies
           :copy-configs
           :custom-lint-fn
           :file-analyzed-fn
           :skip-lint
           :debug]
    :or {cache true}}]
  (let [start-time (System/currentTimeMillis)
        cfg-dir
        (cond config-dir (io/file config-dir)
              filename (core-impl/config-dir filename)
              :else
              (core-impl/config-dir (io/file (System/getProperty "user.dir"))))
        ;; for backward compatibility non-sequential config should be wrapped into collection
        config (core-impl/resolve-config cfg-dir (if (sequential? config) config [config]) debug)
        classpath (:classpath config)
        config (dissoc config :classpath)
        cache-dir (when cache (core-impl/resolve-cache-dir cfg-dir cache cache-dir))
        files (atom 0)
        findings (atom [])
        analysis-cfg (get config :analysis (get-in config [:output :analysis]))
        analyze-var-usages? (get analysis-cfg :var-usages true)
        analyze-var-defs-shallowly? (get-in analysis-cfg [:var-definitions :shallow])
        analyze-locals? (get analysis-cfg :locals)
        analyze-keywords? (get analysis-cfg :keywords)
        analyze-protocol-impls? (get analysis-cfg :protocol-impls)
        analyze-instance-invocations? (get analysis-cfg :instance-invocations)
        analysis-var-meta (some-> analysis-cfg :var-definitions :meta)
        analysis-ns-meta (some-> analysis-cfg :namespace-definitions :meta)
        analysis-context (some-> analysis-cfg :context)
        analyze-java-class-defs? (some-> analysis-cfg :java-class-definitions)
        analyze-java-class-usages? (some-> analysis-cfg :java-class-usages)
        analyze-meta? (or analysis-var-meta analysis-ns-meta)
        analysis (when analysis-cfg
                   (atom (cond-> {:namespace-definitions []
                                  :namespace-usages []
                                  :var-definitions []}
                           analyze-var-usages? (assoc :var-usages [])
                           analyze-locals? (assoc :locals []
                                                  :local-usages [])
                           analyze-keywords? (assoc :keywords [])
                           analyze-protocol-impls? (assoc :protocol-impls [])
                           analyze-java-class-defs? (assoc :java-class-definitions [])
                           analyze-java-class-usages? (assoc :java-class-usages [])
                           analyze-instance-invocations? (assoc :instance-invocations []))))
        used-nss (atom {:clj #{}
                        :cljs #{}
                        :cljc #{}})

        ctx {:config-hash
             ;; in delay to save time when linting only non-jar files
             (delay (core-impl/config-hash config))
             :dependencies (or dependencies no-warnings)
             :copy-configs copy-configs
             :skip-lint skip-lint
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
             :file-analyzed-fn file-analyzed-fn
             :ignores (atom {})
             :id-gen (when analyze-locals? (atom 0))
             :analyze-var-usages? analyze-var-usages?
             :analyze-locals? analyze-locals?
             :analyze-protocol-impls? analyze-protocol-impls?
             :analyze-keywords? analyze-keywords?
             :analyze-arglists? (get analysis-cfg :arglists)
             :analyze-java-class-defs? analyze-java-class-defs?
             :analyze-java-class-usages? analyze-java-class-usages?
             :analysis-var-meta analysis-var-meta
             :analysis-ns-meta analysis-ns-meta
             :analyze-meta? analyze-meta?
             :analyze-var-defs-shallowly? analyze-var-defs-shallowly?
             :analyze-instance-invocations? analyze-instance-invocations?
             :analysis-context analysis-context
             ;; set of files which should not be flushed into cache
             ;; most notably hook configs, as they can conflict with original sources
             ;; NOTE: we don't allow this to be changed in namespace local
             ;; config, for e.g. the clj-kondo playground
             ;; TODO: :__dangerously-allow-string-hooks should not be able to come in via lib configs
             :allow-string-hooks (-> config :hooks :__dangerously-allow-string-hooks__)
             :debug debug}
        lang (or lang :clj)
        ;; primary file analysis and initial lint
        _ (core-impl/process-files (if parallel
                                     (assoc ctx :parallel parallel)
                                     ctx) lint lang filename)
        ;;_ (prn (some-> analysis deref :java-class-usages))
        ;; _ (prn :used-nss @used-nss)
        idacs (when (or dependencies (not skip-lint) analysis)
                (-> (core-impl/index-defs-and-calls ctx)
                    (cache/sync-cache cfg-dir cache-dir)
                    (overrides)))
        _ (when-not dependencies
            (if skip-lint
              (when analysis
                ;; Still need to call l/lint-var-usages, to have analysis/reg-usage! called.
                ;; Would be more consistent to invert relationship, calling linter from analysis.
                (l/lint-var-usage ctx idacs))
              (do
                (l/lint-var-usage ctx idacs)
                (l/lint-unused-namespaces! ctx)
                (l/lint-unused-private-vars! ctx)
                (l/lint-bindings! ctx)
                (l/lint-unresolved-symbols! ctx)
                (l/lint-unresolved-vars! ctx)
                (l/lint-unused-imports! ctx)
                (l/lint-unresolved-namespaces! ctx)
                (l/lint-discouraged-namespaces! ctx))))
        _ (when custom-lint-fn
            (binding [utils/*ctx* ctx]
              (custom-lint-fn (cond->
                               {:config config
                                :reg-finding!
                                (fn [m]
                                  (findings/reg-finding!
                                   (assoc utils/*ctx*
                                          :lang (or (:lang m)
                                                    (core-impl/lang-from-file
                                                     (:filename m) lang))) m))}
                                analysis-cfg
                                (assoc :analysis @analysis)))))
        all-findings @findings
        grouped-findings (group-by (juxt :filename :row :col :type :cljc) all-findings)
        all-findings (core-impl/filter-findings config grouped-findings)
        all-findings (into [] (dedupe) (sort-by (juxt :filename :row :col) all-findings))
        summary (core-impl/summarize all-findings)
        duration (- (System/currentTimeMillis) start-time)
        summary (assoc summary :duration duration :files @files)]
    (cond->
     {:findings all-findings
      :config config
      :summary summary}
      analysis
      (assoc :analysis @analysis))))

(defn merge-configs
  "Returns the merged configuration of c1 with c2."
  ([& configs]
   (reduce merge-config! nil configs)))

(defn resolve-config
  "Returns the configuration for `cfg-dir` merged with home,
  clj-kondo default configs and optional `config` if provided."
  ([cfg-dir]
   (resolve-config cfg-dir {}))
  ([cfg-dir config]
   (core-impl/resolve-config cfg-dir config false)))

(defn config-hash
  "Return the hash of the provided clj-kondo config."
  [config]
  (-> config
      (dissoc :classpath)
      core-impl/config-hash))

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
