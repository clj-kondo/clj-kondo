(ns clj-kondo.main
  {:no-doc true}
  (:gen-class)
  (:refer-clojure :exclude [run!])
  (:require
   [clj-kondo.impl.analyzer :as ana]
   [clj-kondo.impl.cache :as cache]
   [clj-kondo.impl.config :as config]
   [clj-kondo.impl.linters :as l]
   [clj-kondo.impl.overrides :refer [overrides]]
   [clj-kondo.impl.profiler :as profiler]
   [clj-kondo.impl.rewrite-clj-patch]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str
    :refer [starts-with?
            ends-with?]])
  (:import [java.util.jar JarFile JarFile$JarFileEntry]))

(def dev? (= "true" (System/getenv "CLJ_KONDO_DEV")))

(def ^:private version (str/trim
                        (slurp (io/resource "CLJ_KONDO_VERSION"))))
(set! *warn-on-reflection* true)

;;;; printing

(defn- format-output [config]
  (if-let [^String pattern (-> config :output :pattern)]
    (fn [filename row col level message]
      (-> pattern
          (str/replace "{{filename}}" filename)
          (str/replace "{{row}}" (str row))
          (str/replace "{{col}}" (str col))
          (str/replace "{{level}}" (name level))
          (str/replace "{{LEVEL}}" (str/upper-case (name level)))
          (str/replace "{{message}}" message)))
    (fn [filename row col level message]
      (str filename ":" row ":" col ": " (name level) ": " message))))

(defn- print-version []
  (println (str "clj-kondo v" version)))

(comment
  version)

(defn- print-help []
  (print-version)
  ;; TODO: document config format when stable enough
  (println (format "
Usage: [ --help ] [ --version ] [ --lint <files> ] [ --lang (clj|cljs) ] [ --cache [ <dir> ] ] [ --config <config> ]

Options:

  --lint: a file can either be a normal file, directory or classpath. In the
    case of a directory or classpath, only .clj, .cljs and .cljc will be
    processed. Use - as filename for reading from stdin.

  --lang: if lang cannot be derived from the file extension this option will be
    used.

  --cache: if dir exists it is used to write and read data from, to enrich
    analysis over multiple runs. If no value is provided, the nearest .clj-kondo
    parent directory is detected and a cache directory will be created in it.

  --config: config may be a file or an EDN expression. See
    https://cljdoc.org/d/clj-kondo/clj-kondo/%s/doc/configuration.
" version))
  nil)

(defn- source-file? [filename]
  (or (ends-with? filename ".clj")
      (ends-with? filename ".cljc")
      (ends-with? filename ".cljs")))

;;;; jar processing

(defn- sources-from-jar
  [^String jar-path]
  (let [jar (JarFile. jar-path)
        entries (enumeration-seq (.entries jar))
        entries (filter (fn [^JarFile$JarFileEntry x]
                          (let [nm (.getName x)]
                            (source-file? nm))) entries)]
    (map (fn [^JarFile$JarFileEntry entry]
           {:filename (.getName entry)
            :source (slurp (.getInputStream jar entry))}) entries)))

;;;; dir processing

(defn- sources-from-dir
  [dir]
  (let [files (file-seq dir)]
    (keep (fn [^java.io.File file]
            (let [nm (.getPath file)
                  can-read? (.canRead file)
                  source? (source-file? nm)]
              (cond
                (and can-read? source?)
                {:filename nm
                 :source (slurp file)}
                (and (not can-read?) source?)
                (do (println (str nm ":0:0:") "warning: can't read, check file permissions")
                    nil)
                :else nil)))
          files)))

;;;; file processing

(defn- lang-from-file [file default-language]
  (cond (ends-with? file ".clj")
        :clj
        (ends-with? file ".cljc")
        :cljc
        (ends-with? file ".cljs")
        :cljs
        :else default-language))

(def ^:private cp-sep (System/getProperty "path.separator"))

(defn- classpath? [f]
  (str/includes? f cp-sep))

(defn- process-file [ctx filename default-language]
  (try
    (let [file (io/file filename)]
      (cond
        (.exists file)
        (if (.isFile file)
          (if (ends-with? file ".jar")
            ;; process jar file
            (map #(ana/analyze-input ctx (:filename %) (:source %)
                                     (lang-from-file (:filename %) default-language)
                                     dev?)
                 (sources-from-jar filename))
            ;; assume normal source file
            [(ana/analyze-input ctx filename (slurp filename)
                                (lang-from-file filename default-language)
                                dev?)])
          ;; assume directory
          (map #(ana/analyze-input ctx (:filename %) (:source %)
                                   (lang-from-file (:filename %) default-language)
                                   dev?)
               (sources-from-dir file)))
        (= "-" filename)
        [(ana/analyze-input ctx "<stdin>" (slurp *in*) default-language dev?)]
        (classpath? filename)
        (mapcat #(process-file ctx % default-language)
                (str/split filename
                           (re-pattern cp-sep)))
        :else
        [{:findings [{:level :warning
                      :filename filename
                      :col 0
                      :row 0
                      :message "file does not exist"}]}]))
    (catch Throwable e
      (if dev? (throw e)
          [{:findings [{:level :warning
                        :filename filename
                        :col 0
                        :row 0
                        :message "could not process file"}]}]))))

(defn- process-files [ctx files default-lang]
  (mapcat #(process-file ctx % default-lang) files))

;;;; find cache/config dir

(defn- config-dir
  ([] (config-dir
       (io/file
        (System/getProperty "user.dir"))))
  ([cwd]
   (loop [dir (io/file cwd)]
     (let [cfg-dir (io/file dir ".clj-kondo")]
       (if (.exists cfg-dir)
         (if (.isDirectory cfg-dir)
           cfg-dir
           (throw (Exception. (str cfg-dir " must be a directory"))))
         (when-let [parent (.getParentFile dir)]
           (recur parent)))))))

;;;; parse command line options

(defn- parse-opts [options]
  (let [opts (loop [options options
                    opts-map {}
                    current-opt nil]
               (if-let [opt (first options)]
                 (if (starts-with? opt "--")
                   (recur (rest options)
                          (assoc opts-map opt [])
                          opt)
                   (recur (rest options)
                          (update opts-map current-opt conj opt)
                          current-opt))
                 opts-map))
        default-lang (case (first (get opts "--lang"))
                       "clj" :clj
                       "cljs" :cljs
                       "cljc" :cljc
                       :clj)
        cache-opt (get opts "--cache")]
    {:files (get opts "--lint")
     :cache (when cache-opt
              (or (first cache-opt) true))
     :lang default-lang
     :config (first (get opts "--config"))
     :version (get opts "--version")
     :help (get opts "--help")}))

;;;; process config

(defn- resolve-config [cfg-dir config]
  (reduce config/merge-config! config/default-config
          [(when cfg-dir
             (let [f (io/file cfg-dir "config.edn")]
               (when (.exists f)
                 (edn/read-string (slurp f)))))
           (when config
             (if (str/starts-with? config "{")
               (edn/read-string config)
               (edn/read-string (slurp config))))]))

;;;; process cache

(def ^:private empty-cache-opt-warning "WARNING: --cache option didn't specify directory, but no .clj-kondo directory found. Continuing without cache. See https://github.com/borkdude/clj-kondo/blob/master/README.md#project-setup.")

(defn- resolve-cache-dir [cfg-dir cache-dir]
  (when cache-dir
    (if (true? cache-dir)
      (if cfg-dir (io/file cfg-dir ".cache" version)
          (do (println empty-cache-opt-warning)
              nil))
      (io/file cache-dir version))))

;;;; index defs and calls by language and namespace

(defn- mmerge
  "Merges maps no deeper than two levels"
  [a b]
  (merge-with merge a b))

(defn- index-defs-and-calls [defs-and-calls]
  (reduce
   (fn [acc {:keys [:calls :defs :used :lang] :as _m}]
     (-> acc
         (update-in [lang :calls] (fn [prev-calls]
                                    (merge-with into prev-calls calls)))
         (update-in [lang :defs] mmerge defs)
         (update-in [lang :used] into used)))
   {:clj {:calls {} :defs {} :used #{}}
    :cljs {:calls {} :defs {} :used #{}}
    :cljc {:calls {} :defs {} :used #{}}}
   defs-and-calls))

;;;; summary

(def ^:private zinc (fnil inc 0))

(defn- summarize [findings]
  (reduce (fn [acc {:keys [:level]}]
            (update acc level zinc))
          {:error 0 :warning 0 :info 0}
          findings))

;;;; filter/remove output

(defn- filter-findings [config findings]
  (let [print-debug? (:debug config)
        filter-output (not-empty (-> config :output :include-files))
        remove-output (not-empty (-> config :output :exclude-files))]
    (for [{:keys [:filename :level :type] :as f} findings
          :let [level (or (when type (-> config :linters type :level))
                          level)]
          :when (and level (not= :off level))
          :when (if (= :debug type)
                  print-debug?
                  true)
          :when (if filter-output
                  (some (fn [pattern]
                          (re-find (re-pattern pattern) filename))
                        filter-output)
                  true)
          :when (not-any? (fn [pattern]
                            (re-find (re-pattern pattern) filename))
                          remove-output)]
      (assoc f :level level))))

;;;; API

(defn run!
  "TODO: docstring"
  [{:keys [:files
           :lang
           :cache
           :config]}]
  (let [cfg-dir (config-dir)
        config (resolve-config cfg-dir config)
        cache-dir (resolve-cache-dir cfg-dir cache)
        findings (atom [])
        ctx {:config config
             :findings findings
             :namespaces (atom {})}
        processed
        (process-files ctx files lang)
        idacs (index-defs-and-calls processed)
        ;; _ (prn "IDACS" idacs)
        idacs (cache/sync-cache idacs cache-dir)
        idacs (overrides idacs)
        linted-calls (doall (l/lint-calls ctx idacs))
        _ (l/lint-unused-namespaces! ctx)
        _ (l/lint-unused-bindings! ctx)
        all-findings (concat linted-calls (mapcat :findings processed)
                             @findings)
        all-findings (filter-findings config all-findings)]
    {:findings all-findings
     :config config}))

(defn print-findings! [{:keys [:config :findings]}]
  (let [format-fn (format-output config)]
    (doseq [{:keys [:filename :message
                    :level :row :col] :as _finding}
            (dedupe (sort-by (juxt :filename :row :col) findings))]
      (println (format-fn filename row col level message)))))

;;;; main

(defn main
  [& options]
  (try
    (profiler/profile
     :main
     (let [start-time (System/currentTimeMillis)
           {:keys [:help :files :version] :as parsed} (parse-opts options)]
       (or (cond version
                 (print-version)
                 help
                 (print-help)
                 (empty? files)
                 (print-help)
                 :else (let [{findings :findings
                              config :config
                              :as results} (run! parsed)
                             {:keys [:error :warning]} (summarize findings)]
                         (when (-> config :output :show-progress)
                           (println))
                         (print-findings! results)
                         (printf "linting took %sms, "
                                 (- (System/currentTimeMillis) start-time))
                         (println (format "errors: %s, warnings: %s" error warning))
                         (cond (pos? error) 3
                               (pos? warning) 2
                               :else 0)))
           0)))
    (finally
      (profiler/print-profile :main))))

(defn -main [& options]
  (let [exit-code
        (try (apply main options)
             (catch Throwable e
               (if dev? (throw e)
                   (do
                     ;; can't use clojure.stacktrace here, due to
                     ;; https://dev.clojure.org/jira/browse/CLJ-2502
                     (println "Unexpected error. Please report an issue.")
                     (.printStackTrace e)
                     ;; unexpected error
                     124))))]
    (flush)
    (System/exit exit-code)))

;;;; Scratch

(comment
  )
