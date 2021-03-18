(ns clj-kondo.main
  {:no-doc true}
  (:gen-class)
  (:require
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

  --run-as-pod: run clj-kondo as a babashka pod

  --parallel: lint sources in parallel.

  --no-warnings: don't report warnings. Useful for when populating cache.
" core-impl/version))
  nil)

;;;; parse command line options

(defn opt-type [opt]
  (case opt
    "--help"       :scalar
    "--version"    :scalar
    "--lang"       :scalar
    "--cache"      :scalar
    "--cache-dir"  :scalar
    "--config-dir" :scalar
    "--lint"       :coll
    "--config"     :coll
    "--parallel"   :scalar
    "--filename"   :scalar
    "--no-warnings" :scalar
    "--copy-hooks" :scalar
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
     :no-warnings (contains? opts "--no-warnings")
     :copy-hooks (contains? opts "--copy-hooks")}))

(defn main
  [& options]
  (let [{:keys [:help :lint :version :pod :no-warnings] :as parsed}
        (parse-opts options)]
    (or (cond version
              (print-version)
              help
              (print-help)
              pod (pod/run-pod)
              (empty? lint)
              (print-help)
              :else (let [{:keys [:summary]
                           :as results} (clj-kondo/run! parsed)
                          {:keys [:error :warning]} summary]
                      (when-not no-warnings
                        (clj-kondo/print! results))
                      (cond (pos? error) 3
                            (pos? warning) 2
                            :else 0)))
        0)))

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
