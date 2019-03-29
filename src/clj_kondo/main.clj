(ns clj-kondo.main
  (:gen-class)
  (:require [clj-kondo.impl.linters :refer [process-input]]
            [clj-kondo.impl.vars :refer [arity-findings]]
            [clojure.java.io :as io]
            [cognitect.transit :as transit]
            [clojure.string :as str
             :refer [starts-with?
                     ends-with?]])
  (:import [java.util.jar JarFile JarFile$JarFileEntry]))

(def ^:private version (str/trim (slurp (io/resource "CLJK_VERSION"))))
(set! *warn-on-reflection* true)

;;;; printing

(defn- print-findings [findings]
  (doseq [{:keys [:filename :type :message :level :row :col]} findings
          :when level]
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
            (map #(process-input (:source %) (:filename %)
                                 (lang-from-file (:filename %) default-language))
                 (sources-from-jar filename))
            ;; assume normal source file
            [(process-input (slurp filename) filename (lang-from-file filename default-language))])
          ;; assume directory
          (map #(process-input (:source %) (:filename %)
                               (lang-from-file (:filename %) default-language))
               (sources-from-dir file)))
        (= "-" filename)
        [(process-input (slurp *in*) "<stdin>" default-language)]
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
  ([] (config-dir (io/file (System/getProperty "user.dir"))))
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

;;;; main

(defn -main [& options]
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
        [^java.io.File clj-cache-file
         ^java.io.File cljs-cache-file]
        (when cache-opt
          (let [cd (or (when-let [cd (first (get opts "--cache"))]
                         (io/file cd cache-format))
                       (io/file (config-dir) cache-dir))]
            (when-not (.exists cd)
              (io/make-parents (io/file cd "dummy")))
            [(io/file cd (str "clj.transit.json"))
             (io/file cd (str "cljs.transit.json"))]))
        files (get opts "--lint")]
    (cond (get opts "--version")
          (print-version)
          (get opts "--help")
          (print-help)
          (empty? files)
          (print-help))
    :else (let [all-findings (mapcat #(process-file % default-lang) files)
                clj-defns (apply merge (map :defns (filter #(= :clj (:lang %)) all-findings)))
                cljs-defns (apply merge (map :defns (filter #(= :cljs (:lang %)) all-findings)))
                all-calls (mapcat :calls all-findings)
                clj-calls? (boolean (some #(= :clj (:lang %)) all-calls))
                cljs-calls? (boolean (some #(= :cljs (:lang %)) all-calls))
                cache-clj (when (and clj-calls? clj-cache-file (.exists clj-cache-file))
                            (transit/read (transit/reader
                                           (io/input-stream clj-cache-file) :json)))
                cache-cljs
                (when (and cljs-calls? cljs-cache-file (.exists cljs-cache-file))
                  (transit/read (transit/reader
                                 (io/input-stream cljs-cache-file) :json)))
                all-clj-defns (merge cache-clj clj-defns)
                all-cljs-defns (merge cache-cljs cljs-defns)
                arities (arity-findings all-clj-defns all-cljs-defns all-calls)
                findings (mapcat :findings all-findings)]
            (print-findings (concat findings arities))
            (when (and clj-calls? clj-cache-file)
              (let [bos (java.io.ByteArrayOutputStream. 1024)
                    writer (transit/writer (io/output-stream bos) :json)]
                (transit/write writer all-clj-defns)
                (io/copy (.toByteArray bos) clj-cache-file)))
            (when (and cljs-calls? cljs-cache-file)
              (let [bos (java.io.ByteArrayOutputStream. 1024)
                    writer (transit/writer (io/output-stream bos) :json)]
                (transit/write writer all-cljs-defns)
                (io/copy (.toByteArray bos) cljs-cache-file))))))

;;;; scratch

(comment
  ;; TODO: turn some of these into tests
  (spit "/tmp/id.clj" "(defn foo []\n  (def x 1))")
  (-main "/tmp/id.clj")
  (-main)
  (with-in-str "(defn foo []\n  (def x 1))" (-main "--lint" "-"))
  (with-in-str "(defn foo []\n  `(def x 1))" (-main "--lint" "-"))
  (with-in-str "(defn foo []\n  '(def x 1))" (-main "--lint" "-"))
  (inline-defs (p/parse-string-all "(defn foo []\n  (def x 1))"))
  #_(defn foo []\n  (def x 1))
  (nested-lets (p/parse-string-all "(let [i 10])"))
  (with-in-str "(let [i 10] (let [j 11]))" (-main "--lint" "-"))
  (with-in-str "(let [i 10] 1 (let [j 11]))" (-main "--lint" "-"))
  (with-in-str "(let [i 10] #_1 (let [j 11]))" (-main "--lint" "-"))
  (obsolete-do (p/parse-string-all "(do 1 (do 1 2))"))
  (with-in-str "(do 1)" (-main "--lint" "-"))
  (process-input "(fn [] (do 1 2))")
  (process-input "(let [] 1 2 (do 1 2 3))")
  (process-input "(defn foo [] (do 1 2 3))")
  (process-input "(defn foo [] (fn [] 1 2 3))")

  (with-in-str "(ns foo) (defn foo [x]) (foo)" (-main "--lint" "-"))
  (arity-findings (:arities (process-input "(ns foo) (defn foo [x])(ns bar (:require [foo :refer [foo]]))(foo)" "-")))
  )
