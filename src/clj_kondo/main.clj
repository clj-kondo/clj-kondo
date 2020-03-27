(ns clj-kondo.main
  {:no-doc true}
  (:gen-class)
  (:require
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.impl.core :as core-impl]
   [clj-kondo.impl.profiler :as profiler]
   [clojure.string :as str
    :refer [starts-with?]]))

(set! *warn-on-reflection* true)

;;;; printing

(defn- print-version []
  (println (str "clj-kondo v" core-impl/version)))

(defn- print-help []
  (print-version)
  (println (format "
Usage: [ --help ] [ --version ] [ --lint <files> ] [ --lang (clj|cljs) ] [ --cache [ true | false ] ] [ --cache-dir <dir> ] [ --config <config> ]

Options:

  --lint: a file can either be a normal file, directory or classpath. In the
    case of a directory or classpath, only .clj, .cljs and .cljc will be
    processed. Use - as filename for reading from stdin.

  --lang: if lang cannot be derived from the file extension this option will be
    used.

  --cache-dir: when this option is provided, the cache will be resolved to this
    directory. If --cache is false, this option will be ignored.

  --cache: if false, won't use cache. Otherwise, will try to resolve cache
  using `--cache-dir`. If `--cache-dir` is not set, cache is resolved using the
  nearest `.clj-kondo` directory in the current and parent directories.

  --config: config may be a file or an EDN expression. See
    https://cljdoc.org/d/clj-kondo/clj-kondo/%s/doc/configuration.
" core-impl/version))
  nil)

;;;; parse command line options

(defn- parse-opts [options]
  (let [opts (loop [options options
                    opts-map {}
                    current-opt nil]
               (if-let [opt (first options)]
                 (if (starts-with? opt "--")
                   (recur (rest options)
                          (update opts-map opt (fnil identity []))
                          opt)
                   (recur (rest options)
                          (update opts-map current-opt conj opt)
                          current-opt))
                 opts-map))
        default-lang (when-let [lang-opt (last (get opts "--lang"))]
                       (keyword lang-opt))
        cache-opt (get opts "--cache")]
    #_(binding [*out* *err*]
      (prn "cache opt" cache-opt))
    {:lint (get opts "--lint")
     :cache (if cache-opt
              (if-let [f (last cache-opt)]
                (cond (= "false" f) false
                      (= "true" f) true
                      :else f)
                true)
              true)
     :cache-dir (last (get opts "--cache-dir"))
     :lang default-lang
     :config (last (get opts "--config"))
     :version (get opts "--version")
     :help (get opts "--help")}))

(defn main
  [& options]
  (try
    (profiler/profile
     :main
     (let [{:keys [:help :lint :version] :as parsed}
           (parse-opts options)]
       (or (cond version
                 (print-version)
                 help
                 (print-help)
                 (empty? lint)
                 (print-help)
                 :else (let [{:keys [:summary]
                              :as results} (clj-kondo/run! parsed)
                             {:keys [:error :warning]} summary]
                         (clj-kondo/print! results)
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
  )
