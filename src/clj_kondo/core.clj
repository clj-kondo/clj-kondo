(ns clj-kondo.core
  (:refer-clojure :exclude [run!])
  (:require
   [clj-kondo.impl.cache :as cache]
   [clj-kondo.impl.core :as core-impl]
   [clj-kondo.impl.linters :as l]
   [clj-kondo.impl.overrides :refer [overrides]]
   [clojure.java.io :as io]
   [clojure.string :as str]))

;;;; Public API

(defn print!
  "Prints the result from `run!` to `*out*`. Returns `nil`. Alpha,
  subject to change."
  [{:keys [:config :findings :summary]}]
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
      ;; avoid loading clojure.pprint or bringing in additional libs for coercing to EDN or JSON
      :edn
      (do
        (print "{")
        (print (format ":findings\n [%s]"
                       (str/join ",\n  " findings)))
        (when (:summary output-cfg)
          (print (format ",\n :summary %s"
                         summary)))
        (println "}"))
      :json
      (do
        (print "{")
        (print (format "\"findings\":\n [%s]"
                       (str/join ",\n  "
                                 (map
                                  (fn [finding]
                                    (core-impl/finding->json finding))
                                  findings))))
        (when (:summary output-cfg)
          (let [{:keys [:error :warning :duration]} summary]
            (print (format core-impl/json-summary-format
                           error warning duration))))
        (println "}"))))
  (flush)
  nil)

(defn run!
  "Takes a map with:

  - `:lint`: a seqable of files, directories and/or classpaths to lint.

  - `:lang`: optional, defaults to `:clj`. Sets language for linting
  `*in*`. Supported values: `:clj`, `:cljs` and `:cljc`.

  - `:cache`: optional, defaults to `false`. May be a boolean or the
  directory to use for caching. In case of `true`, the cache dir will
  be resolved using the nearest `.clj-kondo` directory in the current
  and parent directories.

  - `:config`: optional. Map or string representing the config as EDN,
  or a config file.

  In places where a file-like value is expected, either a path as string or a
  `java.io.File` may be passed, except for a classpath which must always be a string.

  Returns a map with `:findings`, a seqable of finding maps, a
  `:summary` of the findings and the `:config` that was used to
  produce those findings. This map can be passed to `print!` to print
  to `*out*`. Alpha, subject to change."
  [{:keys [:lint
           :lang
           :cache
           :config]}]
  (let [start-time (System/currentTimeMillis)
        cfg-dir (core-impl/config-dir)
        config (core-impl/resolve-config cfg-dir config)
        cache-dir (core-impl/resolve-cache-dir cfg-dir cache)
        findings (atom [])
        ctx {:config config
             :findings findings
             :namespaces (atom {})}
        lang (or lang :clj)
        processed
        ;; this is needed to force the namespace atom state
        (doall (core-impl/process-files ctx lint lang))
        idacs (core-impl/index-defs-and-calls ctx processed)
        idacs (cache/sync-cache idacs cache-dir)
        idacs (overrides idacs)
        linted-calls (doall (l/lint-var-usage ctx idacs))
        _ (l/lint-unused-namespaces! ctx)
        _ (l/lint-unused-bindings! ctx)
        _ (l/lint-unresolved-symbols! ctx)
        all-findings (concat linted-calls (mapcat :findings processed)
                             @findings)
        all-findings (core-impl/filter-findings config all-findings)
        all-findings (dedupe (sort-by (juxt :filename :row :col) all-findings))
        summary (core-impl/summarize all-findings)
        duration (- (System/currentTimeMillis) start-time)
        summary (assoc summary :duration duration)]
    {:findings all-findings
     :config config
     :summary summary}))

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
