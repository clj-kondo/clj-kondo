(ns clj-kondo.main
  (:gen-class)
  (:require
   [clj-kondo.impl.cache :as cache]
   [clj-kondo.impl.linters :refer [process-input]]
   [clj-kondo.impl.vars :refer [fn-call-findings]]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.stacktrace :refer [print-stack-trace]]
   [clojure.string :as str
    :refer [starts-with?
            ends-with?]])
  (:import [java.util.jar JarFile JarFile$JarFileEntry]))

(def ^:private version (str/trim
                        (slurp (io/resource "CLJ_KONDO_VERSION"))))
(set! *warn-on-reflection* true)

;;;; printing

(defn- print-findings [findings print-debug?]
  (doseq [{:keys [:filename :type :message
                  :level :row :col] :as finding}
          (sort-by (juxt :filename :row :col) findings)
          :when (if (= :debug type)
                  print-debug?
                  true)]
    (println (str filename ":" row ":" col ": " (name level) ": " message))))

(defn- print-version []
  (println (str "clj-kondo v" version)))

(defn- print-help []
  (print-version)
  ;; TODO: document config format when stable enough
  (println (str "
Usage: [ --help ] [ --version ] [ --cache [ <dir> ] ] [ --lang (clj|cljs) ] [ --lint <files> ]

Options:

  --files: a file can either be a normal file, directory or classpath. In the
    case of a directory or classpath, only .clj, .cljs and .cljc will be
    processed. Use - as filename for reading from stdin.

  --lang: if lang cannot be derived from the file extension this option will be
    used.

  --cache: if dir exists it is used to write and read data from, to enrich
    analysis over multiple runs. If no value is provided, the nearest .clj-kondo
    parent directory is detected and a cache directory will be created in it."))
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
            (let [nm (.getPath file)]
              (when (source-file? nm)
                {:filename nm
                 :source (slurp file)}))) files)))

;;;; file processing

(defn- lang-from-file [file default-language]
  (cond (ends-with? file ".clj")
        :clj
        (ends-with? file ".cljc")
        :cljc
        (ends-with? file ".cljs")
        :cljs
        :else default-language))

(defn- classpath? [f]
  (str/includes? f ":"))

(defn- process-file [filename default-language config]
  (let [file (io/file filename)]
    (cond
      (.exists file)
      (if (.isFile file)
        (if (ends-with? file ".jar")
          ;; process jar file
          (mapcat #(process-input (:filename %) (:source %)
                                  (lang-from-file (:filename %) default-language)
                                  config)
                  (sources-from-jar filename))
          ;; assume normal source file
          (process-input filename (slurp filename)
                         (lang-from-file filename default-language)
                         config))
        ;; assume directory
        (mapcat #(process-input (:filename %) (:source %)
                                (lang-from-file (:filename %) default-language)
                                config)
                (sources-from-dir file)))
      (= "-" filename)
      (process-input "<stdin>" (slurp *in*) default-language config)
      (classpath? filename)
      (mapcat #(process-file % default-language config)
              (str/split filename #":"))
      :else
      [{:findings [{:level :warning
                    :filename filename
                    :col 0
                    :row 0
                    :message "File does not exist"}]}])))

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
        cache-opt (get opts "--cache")
        cfg-dir (config-dir)
        cache-dir (when cache-opt
                    (or (when-let [cd (first (get opts "--cache"))]
                          (io/file cd version))
                        (io/file cfg-dir ".cache" version)))
        files (get opts "--lint")
        config-file (or (first (get opts "--config"))
                        (when cfg-dir
                          (let [f (io/file cfg-dir "config.edn")]
                            (when (.exists f)
                              f))))
        config (when config-file (edn/read-string (slurp config-file)))
        debug? (boolean
                (or (get opts "--debug")
                    (get config :debug?)))
        ignore-comments? (boolean
                          (or (get opts "--ignore-comments")
                              (get config :ignore-comments?)))]
    {:opts opts
     :files files
     :cache-dir cache-dir
     :default-lang default-lang
     :ignore-comments? ignore-comments?
     :debug? debug?}))

;;;; process all files

(defn- process-files [files default-lang config]
  (mapcat #(process-file % default-lang config) files))

;;;; index defns and calls by language and namespace

(defn- index-defns-and-calls [defns-and-calls]
  (reduce
   (fn [acc {:keys [:calls :defns :lang]}]
     (-> acc
         (update-in [lang :calls] (fn [prev-calls]
                                    (merge-with into prev-calls calls)))
         (update-in [lang :defns] merge defns)))
   {:clj {:calls {} :defns {}}
    :cljs {:calls {} :defns {}}
    :cljc {:calls {} :defns {}}}
   defns-and-calls))

;;;; overrides

(defn- overrides [idacs]
  (-> idacs
      (cond-> (get-in idacs '[:cljs :defns cljs.core cljs.core/array])
        (assoc-in '[:cljs :defns cljs.core cljs.core/array :var-args-min-arity] 0)
        (get-in idacs '[:cljs :defns cljs.core cljs.core/apply])
        (assoc-in '[:cljs :defns cljs.core cljs.core/apply :var-args-min-arity] 2))))

;;;; summary

(defn summarize [findings]
  (reduce (fn [acc fd]
            (update acc (:level fd) inc))
          {:error 0 :warning 0}
          findings))

;;;; main

(defn main
  [& options]
  (let [start-time (System/currentTimeMillis)
        {:keys [:opts
                :files
                :default-lang
                :cache-dir
                :debug?
                :ignore-comments?]} (parse-opts options)]
    (or (cond (get opts "--version")
              (print-version)
              (get opts "--help")
              (print-help)
              (empty? files)
              (print-help)
              (not-empty files)
              (let [processed (process-files files default-lang
                                             {:debug? debug?
                                              :ignore-comments? ignore-comments?})
                    idacs (index-defns-and-calls processed)
                    idacs (if cache-dir (cache/sync-cache idacs cache-dir)
                              idacs)
                    idacs (overrides idacs)
                    fcf (fn-call-findings idacs)
                    all-findings (concat fcf (mapcat :findings processed))
                    {:keys [:error :warning]} (summarize all-findings)]
                (print-findings all-findings
                                debug?)
                (printf "linting took %sms, "
                        (- (System/currentTimeMillis) start-time))
                (println (format "errors: %s, warnings: %s" error warning))
                (cond (pos? error) 3
                      (pos? warning) 2
                      :else 0)))
        0)))

(defn -main [& options]
  (let [exit-code
        (try (apply main options)
             (catch Throwable e
               (print-stack-trace e)
               ;; unexpected error
               124))]
    (flush)
    (System/exit exit-code)))

;;;; Scratch

(comment
  (def x (def x 1))
  )
