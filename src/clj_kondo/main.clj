(ns clj-kondo.main
  (:gen-class)
  (:require [clj-kondo.impl.linters :refer [process-input]]
            [clj-kondo.impl.vars :refer [fn-call-findings]]
            [clojure.java.io :as io]
            [clj-kondo.impl.cache :as cache]
            [clojure.string :as str
             :refer [starts-with?
                     ends-with?]]
            [clojure.set :as set])
  (:import [java.util.jar JarFile JarFile$JarFileEntry]))

(def ^:private version (str/trim
                        (slurp (io/resource "CLJ_KONDO_VERSION"))))
(set! *warn-on-reflection* true)

;;;; printing

(defn- print-findings [findings print-debug?]
  (doseq [{:keys [:filename :type :message
                  :level :row :col :debug?] :as finding} findings
          :when (if debug?
                  print-debug?
                  true)]
    (println (str filename ":" row ":" col ": " (name level) ": " message))))

(defn- print-version []
  (println (str "clj-kondo v" version)))

(defn- print-help []
  (print-version)
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
    parent directory is used."))
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
        :clj
        (ends-with? file ".cljs")
        :cljs
        :else default-language))

(defn- classpath? [f]
  (str/includes? f ":"))

(defn- process-file [filename default-language]
  (try
    (let [file (io/file filename)]
      (cond
        (.exists file)
        (if (.isFile file)
          (if (ends-with? file ".jar")
            ;; process jar file
            (map #(process-input (:filename %) (:source %)
                                 (lang-from-file (:filename %) default-language))
                 (sources-from-jar filename))
            ;; assume normal source file
            [(process-input filename (slurp filename) (lang-from-file filename default-language))])
          ;; assume directory
          (map #(process-input (:filename %) (:source %)
                               (lang-from-file (:filename %) default-language))
               (sources-from-dir file)))
        (= "-" filename)
        [(process-input "<stdin>" (slurp *in*) default-language)]
        (classpath? filename)
        (mapcat #(process-file % default-language)
                (str/split filename #":"))
        :else
        [{:findings [{:level :error
                      :filename filename
                      :col 0
                      :row 0
                      :message (str "Can't read " filename ", file does not exist.")}]}]))
    (catch Exception e
      (let [filename (if (= "-" filename)
                       (or (and (.exists (io/file filename))
                                filename)
                           "<stdin>")
                       filename)]
        [{:findings [{:level :error
                      :filename filename
                      :col 0
                      :row 0
                      :message (str "Can't read "
                                    filename ", "
                                    (.getMessage e))}]}]))))

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

;;;; cache

(def ^:private cache-format "v1")
(def ^:private cache-dir
  (str ".cache/" cache-format))

;;;; synchronize namespaces with cache

(defn- sync-cache [idacs cache-dir]
  (cache/with-cache cache-dir 6
    (reduce (fn [idacs lang]
              (let [analyzed-namespaces
                    (set (keys (get-in idacs [lang :defns])))
                    called-namespaces
                    (conj (set (keys (get-in idacs [lang :calls])))
                          (case lang
                            :clj 'clojure.core
                            :cljs 'cljs.core))
                    load-from-clj-cache
                    (set/difference called-namespaces analyzed-namespaces)
                    defns-from-cache
                    (when cache-dir
                      (reduce (fn [acc ns-sym]
                                (if-let [data (cache/from-cache cache-dir
                                                                lang ns-sym)]
                                  (assoc acc ns-sym
                                         data)
                                  acc))
                              {} load-from-clj-cache))]
                (when cache-dir
                  (doseq [ns-name analyzed-namespaces]
                    (let [ns-data (get-in idacs [lang :defns ns-name])]
                      (cache/to-cache cache-dir lang ns-name ns-data))))
                (update-in idacs [lang :defns]
                           merge defns-from-cache)))
            idacs
            [:clj :cljs])))

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
                       :clj)
        cache-opt (get opts "--cache")
        cache-dir (when cache-opt
                    (or (when-let [cd (first (get opts "--cache"))]
                          (io/file cd cache-format))
                        (io/file (config-dir) cache-dir)))
        files (get opts "--lint")
        debug? (get opts "--debug")]
    {:opts opts
     :files files
     :cache-dir cache-dir
     :default-lang default-lang
     :debug? debug?}))

;;;; process all files

(defn- process-files [files default-lang]
  (mapcat #(process-file % default-lang) files))

;;;; index defns and calls by language and namespace

(defn- index-defns-and-calls [defns-and-calls]
  (reduce
   (fn [acc {:keys [:calls :defns :lang]}]
     (-> acc
         (update-in [lang :calls] merge calls)
         (update-in [lang :defns] merge defns)))
   {:clj {:calls {} :defns {}}
    :cljs {:calls {} :defns {}}}
   defns-and-calls))

;;;; main

(defn -main [& options]
  (let [{:keys [:opts
                :files
                :default-lang
                :cache-dir
                :debug?]} (parse-opts options)]
    (cond (get opts "--version")
          (print-version)
          (get opts "--help")
          (print-help)
          (empty? files)
          (print-help))
    :else (let [all-findings (process-files files default-lang)
                idacs (index-defns-and-calls all-findings)
                idacs (if cache-dir (sync-cache idacs cache-dir)
                          idacs)
                afs (fn-call-findings idacs)]
            (print-findings (concat afs (mapcat :findings all-findings))
                            debug?))))

;;;; Scratch

(comment

  )
