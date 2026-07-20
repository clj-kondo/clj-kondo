(ns clj-kondo.impl.suppressions
  {:no-doc true}
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   [java.io File]
   [java.nio.file AtomicMoveNotSupportedException CopyOption Files StandardCopyOption]
   [java.util UUID]))

(set! *warn-on-reflection* true)

(def ^:private current-version 1)

(defn suppression-file [config-dir suppressions-location]
  (let [default-dir (or config-dir
                        (io/file (System/getProperty "user.dir") ".clj-kondo"))
        location (some-> suppressions-location io/file)
        directory? (and location
                        (or (.isDirectory ^File location)
                            (str/ends-with? (str suppressions-location) "/")
                            (str/ends-with? (str suppressions-location) "\\")))]
    (if directory?
      (io/file location
               (str "suppressions_"
                    (Integer/toUnsignedString
                     (hash (.getAbsolutePath ^File (io/file default-dir)))
                     16)
                    ".edn"))
      (io/file (or location
                   (io/file default-dir "suppressions.edn"))))))

(defn normalize-filename [root filename]
  (if (or (nil? root)
          (= "<stdin>" filename)
          (str/starts-with? filename "jar:")
          (str/includes? filename "!/"))
    filename
    (try
      (let [root-path (-> (io/file (str root)) .getAbsoluteFile .toPath .normalize)
            filename-path (-> (io/file filename) .getAbsoluteFile .toPath .normalize)]
        (-> (.relativize root-path filename-path)
            str
            (str/replace "\\" "/")))
      (catch IllegalArgumentException _
        (str/replace filename "\\" "/")))))

(defn- suppression-key [{:keys [filename type]}]
  [filename type])

(defn- finding-key [root {:keys [filename type]}]
  [(normalize-filename root filename) type])

(defn- suppressible-finding? [{:keys [filename level type]}]
  (and (= :error level)
       (some? type)
       (not= "<stdin>" filename)))

(defn- valid-entry? [{:keys [filename type count] :as entry}]
  (and (= #{:filename :type :count} (set (keys entry)))
       (string? filename)
       (keyword? type)
       (pos-int? count)))

(defn read-suppressions [file]
  (when (.exists (io/file file))
    (let [{:keys [version suppressions] :as data}
          (edn/read-string (slurp file))]
      (when-not (and (= current-version version)
                     (= #{:version :suppressions} (set (keys data)))
                     (vector? suppressions)
                     (every? valid-entry? suppressions)
                     (apply distinct? (map suppression-key suppressions)))
        (throw (ex-info (str "Invalid suppressions file: " file)
                        {:file (str file)})))
      suppressions)))

(defn- sort-entries [entries]
  (->> entries
       (sort-by (juxt :filename (comp str :type)))
       vec))

(defn counts->entries [counts]
  (->> counts
       (map (fn [[[filename type] count]]
              {:filename filename
               :type type
               :count count}))
       sort-entries))

(defn findings->entries
  ([findings]
   (findings->entries findings nil))
  ([findings root]
   (->> findings
        (filter suppressible-finding?)
        (map (partial finding-key root))
        frequencies
        counts->entries)))

(defn replace-suppressions
  ([entries findings suppress-rules]
   (replace-suppressions entries findings suppress-rules nil))
  ([entries findings suppress-rules root]
   (let [suppress-rules (not-empty (set suppress-rules))
         generated-entries (findings->entries
                            (if suppress-rules
                              (filter (comp suppress-rules :type) findings)
                              findings)
                            root)
         generated-scopes (into #{} (map (juxt :filename :type)) generated-entries)
         retained-entries (remove
                           (fn [{:keys [filename type]}]
                             (contains? generated-scopes [filename type]))
                           entries)]
     (sort-entries (concat retained-entries generated-entries)))))

(defn apply-suppressions
  ([findings entries]
   (apply-suppressions findings entries nil))
  ([findings entries root]
   (apply-suppressions findings entries root (map :filename findings)))
  ([findings entries root analyzed-filenames]
   (let [limits (into {} (map (juxt suppression-key :count)) entries)
         suppressible-findings (filter suppressible-finding? findings)
         counts (frequencies
                 (map (partial finding-key root) suppressible-findings))
         analyzed-files (into #{}
                              (map (partial normalize-filename root))
                              analyzed-filenames)
         suppressed-keys
         (into #{}
               (keep (fn [[k count]]
                       (when (and (contains? limits k)
                                  (<= count (get limits k)))
                         k))
                     counts))
         used
         (into {}
               (keep (fn [[k limit]]
                       (when-let [count (get counts k)]
                         [k (min count limit)]))
                     limits))
         unused
         (into {}
               (keep (fn [[k limit]]
                       (let [[filename] k
                             count (get counts k 0)]
                         (when (and (contains? analyzed-files filename)
                                    (< count limit))
                           [k (- limit count)])))
                     limits))
         suppressed? (fn [finding]
                       (and (suppressible-finding? finding)
                            (contains? suppressed-keys
                                       (finding-key root finding))))]
     {:findings (into []
                      (remove suppressed?)
                      findings)
      :suppressed-findings (into [] (filter suppressed?) findings)
      :used used
      :unused unused})))

(defn- source-file-exists? [root filename]
  (or (= "<stdin>" filename)
      (str/starts-with? filename "jar:")
      (str/includes? filename "!/")
      (.exists ^File (io/file (str root) filename))))

(defn prune-suppressions
  ([entries used]
   (->> entries
        (keep (fn [entry]
                (when-let [used-count (get used (suppression-key entry))]
                  (assoc entry :count used-count))))
        sort-entries))
  ([entries used analyzed-filenames root]
   (let [analyzed-files (into #{}
                              (map (partial normalize-filename root))
                              analyzed-filenames)]
     (->> entries
          (keep (fn [{:keys [filename] :as entry}]
                  (if-let [used-count (get used (suppression-key entry))]
                    (assoc entry :count used-count)
                    (when (and (not (contains? analyzed-files filename))
                               (source-file-exists? root filename))
                      entry))))
          sort-entries))))

(defn- format-suppressions [entries]
  (if (seq entries)
    (str "{:version " current-version
         "\n :suppressions [" (str/join "\n                " (map pr-str entries))
         "]}\n")
    (str "{:version " current-version "\n :suppressions []}\n")))

(defn- move-replacing! [^File source ^File target]
  (try
    (Files/move (.toPath source)
                (.toPath target)
                (into-array CopyOption [StandardCopyOption/ATOMIC_MOVE
                                        StandardCopyOption/REPLACE_EXISTING]))
    (catch AtomicMoveNotSupportedException _
      (Files/move (.toPath source)
                  (.toPath target)
                  (into-array CopyOption [StandardCopyOption/REPLACE_EXISTING])))))

(defn write-suppressions! [file entries]
  (let [^File file (.getAbsoluteFile (io/file file))
        ^File parent (.getParentFile file)
        _ (.mkdirs parent)
        ^File temp-file (io/file parent
                                 (str "." (.getName file) "."
                                      (UUID/randomUUID) ".tmp"))]
    (try
      (spit temp-file (format-suppressions (sort-entries entries)))
      (move-replacing! temp-file file)
      (finally
        (when (.exists temp-file)
          (.delete temp-file))))))
