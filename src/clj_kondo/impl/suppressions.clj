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
  (io/file (or suppressions-location
               (io/file (or config-dir
                            (io/file (System/getProperty "user.dir") ".clj-kondo"))
                        "suppressions.edn"))))

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

(defn- suppression-key [{:keys [filename type message]}]
  [filename type message])

(defn- finding-key [root {:keys [filename type message]}]
  [(normalize-filename root filename) type message])

(defn- valid-entry? [{:keys [filename type message count] :as entry}]
  (and (= #{:filename :type :message :count} (set (keys entry)))
       (string? filename)
       (keyword? type)
       (or (nil? message)
           (string? message))
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
       (sort-by (juxt :filename (comp str :type) :message))
       vec))

(defn findings->entries
  ([findings]
   (findings->entries findings nil))
  ([findings root]
   (->> findings
        (map (partial finding-key root))
        frequencies
        (map (fn [[[filename type message] count]]
               {:filename filename
                :type type
                :message message
                :count count}))
        sort-entries)))

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
   (let [limits (into {} (map (juxt suppression-key :count)) entries)]
     (-> (reduce
          (fn [{:keys [seen] :as result} finding]
            (let [k (finding-key root finding)
                  occurrence (inc (get seen k 0))
                  result (assoc-in result [:seen k] occurrence)]
              (if (<= occurrence (get limits k 0))
                (update-in result [:used k] (fnil inc 0))
                (update result :findings conj finding))))
          {:findings []
           :seen {}
           :used {}}
          findings)
         (dissoc :seen)))))

(defn prune-suppressions [entries used]
  (->> entries
       (keep (fn [entry]
               (when-let [used-count (get used (suppression-key entry))]
                 (assoc entry :count used-count))))
       sort-entries))

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
