(ns clj-kondo.main
  {:no-doc true}
  (:gen-class)
  (:require
   [aaaa-this-has-to-be-first.because-patches]
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.impl.core :as core-impl]
   [clojure.string :as str
    :refer [starts-with?]]
   [pod.borkdude.clj-kondo :as pod]))

(set! *warn-on-reflection* true)

;;;; printing

(defn- print-version []
  (println (str "clj-kondo v" core-impl/version)))

(defn- print-help []
  (print-version)
  (println (format "

Options:

  --lint <file>: a file can either be a normal file, directory or classpath. In the
    case of a directory or classpath, only .clj, .cljs and .cljc will be
    processed. Use - as filename for reading from stdin.

  --lang <lang>: if lang cannot be derived from the file extension this option will be
    used. Supported values: clj, cljs, cljc.

  --filename <file>: in case stdin is used for linting, use this to set the
    reported filename.

  --cache-dir: when this option is provided, the cache will be resolved to this
    directory. If --cache is false, this option will be ignored.

  --cache: if false, won't use cache. Otherwise, will try to resolve cache
  using `--cache-dir`. If `--cache-dir` is not set, cache is resolved using the
  nearest `.clj-kondo` directory in the current and parent directories.

  --config <config>: config may be a file or an EDN expression. See
    https://cljdoc.org/d/clj-kondo/clj-kondo/%s/doc/configuration

  --config-dir <config-dir>: use this config directory instead of auto-detected
    .clj-kondo dir.

  --parallel: lint sources in parallel.

  --dependencies: don't report any findings. Useful for populating cache while linting dependencies.

  --copy-configs: copy configs from dependencies while linting.

  --skip-lint: skip lint/analysis, still check for other tasks like copy-configs.

  --fail-level <level>: minimum severity for exit with error code.  Supported values:
    warning, error.  The default level if unspecified is warning.

  --debug: print debug information.
" core-impl/version))
  nil)

;;;; parse command line options

(defn opt-type [opt]
  (case opt
    "--help"         :scalar
    "--version"      :scalar
    "--lang"         :scalar
    "--cache"        :scalar
    "--cache-dir"    :scalar
    "--config-dir"   :scalar
    "--lint"         :coll
    "--config"       :coll
    "--parallel"     :scalar
    "--filename"     :scalar
    "--no-warnings"  :scalar ;; deprecated
    "--dependencies" :scalar
    "--copy-configs" :scalar
    "--skip-lint"    :scalar
    "--fail-level"   :scalar
    "--debug"        :scalar
    :scalar))

(defn- parse-opts [options]
  (let [opts (loop [options options
                    opts-map {}
                    current-opt nil]
               (if-let [opt (first options)]
                 (if (starts-with? opt "--")
                   (recur (rest options)
                          ;; assoc nil value to indicate opt as explicitly added via cli args
                          (case (opt-type opt)
                            :scalar (assoc opts-map opt nil)
                            :coll (update opts-map opt identity))
                          opt)
                   (recur (rest options)
                          (update opts-map current-opt (fnil conj []) opt)
                          current-opt))
                 opts-map))
        default-lang (when-let [lang-opt (last (get opts "--lang"))]
                       (keyword lang-opt))
        cache-opt? (contains? opts "--cache")]
    {:lint (distinct (get opts "--lint"))
     :filename (last (get opts "--filename"))
     :cache (if cache-opt?
              (if-let [f (last (get opts "--cache"))]
                (cond (= "false" f) false
                      (= "true" f) true
                      :else f)
                true)
              true)
     :cache-dir (last (get opts "--cache-dir"))
     :lang default-lang
     :config (get opts "--config")
     :config-dir (last (get opts "--config-dir"))
     :version (contains? opts "--version")
     :help (contains? opts "--help")
     :pod (= "true" (System/getenv "BABASHKA_POD"))
     :parallel (let [[k v] (find opts "--parallel")]
                 (when k
                   (or (nil? v)
                       (= "true" v))))
     :dependencies (or (contains? opts "--dependencies")
                       (contains? opts "--no-warnings") ;; deprecated
                       ,)
     :copy-configs (contains? opts "--copy-configs")
     :skip-lint (contains? opts "--skip-lint")
     :fail-level (or (last (get opts "--fail-level"))
                     "warning")
     :debug (contains? opts "--debug")}))

(def fail-level? #{"warning" "error"})

(defn main
  [& options]
  (let [{:keys [:help :lint :version :pod :dependencies :fail-level] :as parsed}
        (parse-opts options)]
    (or (cond version
              (print-version)
              help
              (print-help)
              pod (pod/run-pod)
              (empty? lint)
              (print-help)
              (not (fail-level? fail-level))
              (print-help)
              :else (let [{:keys [:summary]
                           :as results} (clj-kondo/run! parsed)
                          {:keys [:error :warning]} summary]
                      (when-not dependencies
                        (clj-kondo/print! results))
                      (cond
                        (= "warning" fail-level)
                        (cond (pos? error) 3
                              (pos? warning) 2
                              :else 0)
                        (= "error" fail-level)
                        (if (pos? error)
                          3
                          0))))
        0)))

(def musl?
  "Captured at compile time, to know if we are running inside a
  statically compiled executable with musl."
  (and (= "true" (System/getenv "CLJ_KONDO_STATIC"))
       (= "true" (System/getenv "CLJ_KONDO_MUSL"))))

(defmacro run [expr]
  (if musl?
    ;; When running in musl-compiled static executable we lift execution of bb
    ;; inside a thread, so we have a larger than default stack size, set by an
    ;; argument to the linker. See https://github.com/oracle/graal/issues/3398
    `(let [v# (volatile! nil)
           f# (fn []
                (vreset! v# ~expr))]
       (doto (Thread. nil f# "main")
         (.start)
         (.join))
       @v#)
    `(do ~expr)))

(defn -main [& options]
  (let [exit-code
        (try (apply main options)
             (catch Throwable e
               (if core-impl/dev? (throw e)
                   (do
                     ;; can't use clojure.stacktrace here, due to
                     ;; https://dev.clojure.org/jira/browse/CLJ-2502
                     (binding [*out* *err*]
                       (println "Unexpected error. Please report an issue."))
                     (.printStackTrace e)
                     ;; unexpected error
                     124))))]
    (flush)
    (System/exit exit-code)))

;;;; Scratch

(comment

  (into #{}
   (comp
    (map :filename)
    (distinct))
   (:findings (read-string (with-out-str
                             (main "--lint" "corpus/case.clj"
                                   "--lint" "corpus/defmulti.clj"
                                   "--config" "{:output {:format :edn}}"
                                   "--config" "{:linters {:invalid-arity {:level :warning}}}")))))

  )
