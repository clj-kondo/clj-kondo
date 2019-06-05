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
  "Prints the result from `run!` to `*out`. Returns `nil`."
  [{:keys [:config :findings]}]
  (case (-> config :output :format)
    :text
    (let [format-fn (core-impl/format-output config)]
      (doseq [{:keys [:filename :message
                      :level :row :col] :as _finding}
              (dedupe (sort-by (juxt :filename :row :col) findings))]
        (println (format-fn filename row col level message))))
    ;; avoid loading clojure.pprint or bringing in additional libs for coercing to EDN or JSON
    :edn
    (println {:findings (format "\n [%s]" (str/join ",\n  " findings))})
    :json
    (let [row-format "{\"type\":\"%s\", \"filename\":\"%s\", \"row\":%s,\"col\":%s, \"level\":\"%s\", \"message\":\"%s\"}"]
      (println
       (format "{\"findings\":\n [%s]}"
               (str/join ",\n  "
                         (map
                          (fn [{:keys [:filename :type :message
                                       :level :row :col]}]
                            (format row-format
                                    (name type) filename row
                                    col (name level)
                                    message))
                          findings))))))
  nil)

(defn run!
  "Takes a map with:

  - `:files`: seqable of files, directories and/or classpaths to lint.

  - `:lang`: optional, defaults to `:clj`. Sets language for linting
  `*in`. Supported values: `:clj`, `:cljs` and `:cljc`.

  - `:cache`: optional, defaults to `false`. Supported values: a
  boolean or the directory to use for caching. In case of `true`, the
  cache dir will be resolved using the nearest `.clj-kondo` directory
  in the current and parent directories.

  - `:config`: optional. Map or string representing the config as EDN,
  or a config file.

  In places where a file-like value is expected, either a path as string or a
  `java.io.File` may be passed, except for a classpath which must always be a string.

  Returns a map with `:findings`, a seqable of finding maps and the
  `:config` that was used to produce those findings. This map can be
  passed to `print!` to print to `*out*`."
  [{:keys [:files
           :lang
           :cache
           :config]}]
  (let [cfg-dir (core-impl/config-dir)
        config (core-impl/resolve-config cfg-dir config)
        cache-dir (core-impl/resolve-cache-dir cfg-dir cache)
        findings (atom [])
        ctx {:config config
             :findings findings
             :namespaces (atom {})}
        processed
        (core-impl/process-files ctx files lang)
        idacs (core-impl/index-defs-and-calls processed)
        idacs (cache/sync-cache idacs cache-dir)
        idacs (overrides idacs)
        linted-calls (doall (l/lint-calls ctx idacs))
        _ (l/lint-unused-namespaces! ctx)
        _ (l/lint-unused-bindings! ctx)
        all-findings (concat linted-calls (mapcat :findings processed)
                             @findings)
        all-findings (core-impl/filter-findings config all-findings)]
    {:findings all-findings
     :config config}))

;;;; Scratch

(comment
  (require '[clj-kondo.core :as clj-kondo])
  (def res (clj-kondo/run!
            {;; seq of string or file
             :files ["corpus" (io/file "test")]
             :config {:linters {:invalid-arity {:level :off}}}
             ;; :cache takes a string, file or boolean
             :cache (io/file "/tmp/clj-kondo-cache")
             ;; only relevant when linting stdin
             :lang :clj}))
  (first (:findings res))
  (clj-kondo/print-findings! res)
  )
