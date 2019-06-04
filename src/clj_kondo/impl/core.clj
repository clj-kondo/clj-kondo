(ns clj-kondo.impl.core
  "Implementation details of clj-kondo.core"
  {:no-doc true}
  (:require
   [clj-kondo.impl.config :as config]
   [clj-kondo.impl.analyzer :as ana]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import [java.util.jar JarFile JarFile$JarFileEntry]))

(set! *warn-on-reflection* true)

(def dev? (= "true" (System/getenv "CLJ_KONDO_DEV")))

(def version
  (str/trim
   (slurp (io/resource "CLJ_KONDO_VERSION"))))

(defn format-output [config]
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

;;;; process config

(defn resolve-config [cfg-dir config]
  (reduce config/merge-config! config/default-config
          [(when cfg-dir
             (let [f (io/file cfg-dir "config.edn")]
               (when (.exists f)
                 (edn/read-string (slurp f)))))
           (when config
             (cond (map? config) config
                   (str/starts-with? config "{")
                   (edn/read-string config)
                   :else (edn/read-string (slurp config))))]))

;;;; process cache

(def empty-cache-opt-warning "WARNING: --cache option didn't specify directory, but no .clj-kondo directory found. Continuing without cache. See https://github.com/borkdude/clj-kondo/blob/master/README.md#project-setup.")

(defn resolve-cache-dir [cfg-dir cache-dir]
  (when cache-dir
    (if (true? cache-dir)
      (if cfg-dir (io/file cfg-dir ".cache" version)
          (do (println empty-cache-opt-warning)
              nil))
      (io/file cache-dir version))))

;;;; find cache/config dir

(defn config-dir
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

;;;; jar processing

(defn source-file? [filename]
  (or (str/ends-with? filename ".clj")
      (str/ends-with? filename ".cljc")
      (str/ends-with? filename ".cljs")))

(defn sources-from-jar
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

(defn sources-from-dir
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

(defn lang-from-file [file default-language]
  (cond (str/ends-with? file ".clj")
        :clj
        (str/ends-with? file ".cljc")
        :cljc
        (str/ends-with? file ".cljs")
        :cljs
        :else default-language))

(def cp-sep (System/getProperty "path.separator"))

(defn classpath? [f]
  (str/includes? f cp-sep))

(defn process-file [ctx filename default-language]
  (try
    (let [file (io/file filename)]
      (cond
        (.exists file)
        (if (.isFile file)
          (if (str/ends-with? file ".jar")
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

(defn process-files [ctx files default-lang]
  (mapcat #(process-file ctx % default-lang) files))

;;;; index defs and calls by language and namespace

(defn mmerge
  "Merges maps no deeper than two levels"
  [a b]
  (merge-with merge a b))

(defn index-defs-and-calls [defs-and-calls]
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

(def zinc (fnil inc 0))

(defn summarize [findings]
  (reduce (fn [acc {:keys [:level]}]
            (update acc level zinc))
          {:error 0 :warning 0 :info 0}
          findings))

;;;; filter/remove output

(defn filter-findings [config findings]
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
