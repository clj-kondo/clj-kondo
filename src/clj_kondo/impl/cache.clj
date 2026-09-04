(ns clj-kondo.impl.cache
  {:no-doc true}
  (:require
   [babashka.fs :as fs]
   [clj-kondo.impl.types :as types]
   [clj-kondo.impl.utils :refer [one-of]]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [cognitect.transit :as transit])
  (:import [java.io RandomAccessFile]
           [java.util.concurrent.locks ReentrantLock]))

(set! *warn-on-reflection* true)

(def ^ReentrantLock thread-lock (ReentrantLock.))

(defn built-in-cache-resource [lang ns-sym]
  (io/resource (str "clj_kondo/impl/cache/built_in/"
                    (name lang) "/" ns-sym ".transit.json")))

(defn cache-file ^java.io.File [cache-dir lang ns-sym]
  (io/file cache-dir (name lang) (str ns-sym ".transit.json")))

(defn from-cache-1 [cache-dir lang ns-sym]
  (when-let [{:keys [resource source]}
             (or (when cache-dir
                   (let [f (cache-file cache-dir lang ns-sym)]
                     (when (.exists f)
                       {:source :disk
                        :resource f})))
                 (when-let [resource (built-in-cache-resource lang ns-sym)]
                   {:source :built-in
                    :resource resource}))]
    (assoc
     (with-open [is (io/input-stream resource)]
       (transit/read (transit/reader is :json)))
     :source source)))

(defn skip-write?
  [^java.io.File config-dir ^String filename]
  (when filename
    (or (str/includes? filename "clj-kondo.exports")
        ;; this depends on clj-kondo's way of denoting a jar + entry when a jar
        ;; file entry doesn't contain clj-kondo.exports, then we should not skip
        ;; we need to check this before converting it into a nio Path, which
        ;; fails on Windows.
        (when-not (or (str/includes? filename ".jar:")
                      (= "<stdin>" filename))
          #_:clj-kondo/ignore
          (try (.startsWith (-> (.toPath (io/file filename))
                                (.toAbsolutePath))
                            (-> (.toPath config-dir)
                                (.toAbsolutePath)))
               #_(catch Exception _ false))))))

(defn no-flush-output-stream
  "See https://github.com/cognitect/transit-clj/issues/43#issuecomment-1650341353"
  ^java.io.OutputStream [^java.io.OutputStream os]
  (proxy [java.io.BufferedOutputStream] [os]
    (flush [])
    (close []
      (let [^java.io.BufferedOutputStream this this]
        (proxy-super flush)
        (proxy-super close)))))

(defn to-cache
  "Writes ns-data to cache-dir. Always use with `with-cache`."
  [config-dir cache-dir lang ns-sym ns-data]
  (let [filename (:filename ns-data)]
    (when-not (skip-write? config-dir filename)
      (let [file (cache-file cache-dir lang ns-sym)
            _ (io/make-parents file)
            os (io/output-stream file)]
        (with-open [os (no-flush-output-stream os)]
          (let [writer (transit/writer os :json)]
            (try (transit/write writer ns-data)
                 (catch Exception e
                   (binding [*out* *err*]
                     (println "[clj-kondo] WARNING: could not serialize cache data for namespace" ns-sym))
                   (throw e)))))))))

(def ^:dynamic *lock-file-name* "lock")

(defmacro with-cache
  "Tries to lock cache in the scope of `body`. Retries `max-retries`
  times while sleeping (2^retry)*25 ms in between. If not succeeded
  after retries, throws `Exception`."
  [cache-dir max-retries & body]
  `(let [cache-dir# ~cache-dir]
     (if-not cache-dir#
       (do ~@body)
       (let [lock-file# (io/file cache-dir# *lock-file-name*)]
         (io/make-parents lock-file#)
         (with-open [raf# (RandomAccessFile. lock-file# "rw")
                     channel# (.getChannel raf#)]
           (loop [retry# 0
                  backoff# 25]
             (if-let [lock#
                      (try (.tryLock channel#)
                           (catch java.nio.channels.OverlappingFileLockException _#
                             nil))]
               (try
                 ~@body
                 (finally (.release ^java.nio.channels.FileLock lock#)))
               (if (= retry# ~max-retries)
                 (throw (Exception.
                         (str "Clj-kondo cache is locked by other thread or process.")))
                 (do (Thread/sleep backoff#)
                     (recur (inc retry#)
                            (* 2 backoff#)))))))))))

(defmacro with-thread-lock [& body]
  `(do
     (.lock thread-lock)
     (try ~@body
          (finally (.unlock thread-lock)))))

(defmacro with-named-lock
  "Bind `*lock-file-name*` to `lock-name` and acquire both the thread
  and file-based locks under `cache-dir` for `body`. `max-retries`
  is forwarded to `with-cache`."
  [lock-name cache-dir max-retries & body]
  `(binding [*lock-file-name* ~lock-name]
     (with-thread-lock
       (with-cache ~cache-dir ~max-retries
         ~@body))))

(defn load-when-missing [idacs cache-dir lang ns-sym]
  (if (string? (-> ns-sym meta :raw-name))
    ;; if raw-name is a string, the source is JavaScript, there is no point in
    ;; searching for that
    idacs
    (let [path [lang :defs ns-sym]]
      (if-not (get-in idacs path)
        (if-let [data (from-cache-1 cache-dir lang ns-sym)]
          (let [idacs (update idacs :linted-namespaces conj ns-sym)
                res (assoc-in idacs path data)]
            ;; proxied-namespaces are here because of potemkin/import-vars since
            ;; import-vars only supports clj and not cljs, we're fine with loading
            ;; these namespace only with the current language (which is :clj)
            (if-let [proxied (:proxied-namespaces data)]
              (reduce #(load-when-missing %1 cache-dir lang %2) res proxied)
              res))
          idacs)
        (update idacs :linted-namespaces conj ns-sym)))))

(defn update-defs
  "Resolve types of defs. Optionally store to cache. Return defs with
  resolved types for linting.."
  [idacs config-dir cache-dir lang defs]
  (persistent!
   (reduce-kv (fn [m ns-nm ns-data]
                (let [source (:source ns-data)
                      resolve? (and (not (one-of source [:disk :built-in]))
                                    (seq ns-data))
                      resolve-types (fn [nsd] (types/resolve-types idacs nsd))
                      ns-data
                      (if resolve?
                        (if (identical? lang :cljc)
                          (-> ns-data
                              (update :clj resolve-types)
                              (update :cljs resolve-types))
                          (resolve-types ns-data))
                        ns-data)]
                  ;; (when resolve? (prn ns-data))
                  (when (and cache-dir resolve?)
                    (to-cache config-dir cache-dir lang ns-nm ns-data))
                  (assoc! m ns-nm ns-data)))
              (transient {})
              defs)))

(defn sync-cache*
  "Reads required namespaces from cache and combines them with the
  namespaces we linted in this run."
  [idacs config-dir cache-dir]
  ;; first load all idacs so we can resolve types
  (let [idacs (assoc idacs :linted-namespaces #{})
        idacs
        (reduce (fn [idacs lang]
                  (let [required-namespaces (get-in idacs [:used-namespaces lang])]
                    (reduce (fn [idacs lang]
                              (reduce #(load-when-missing %1 cache-dir lang %2)
                                      idacs
                                      required-namespaces))
                            idacs
                            (case lang
                              (:cljs :cljc) [:clj :cljs :cljc]
                              :clj [:clj :cljc]))))
                idacs
                [:clj :cljs :cljc])
        idacs (reduce (fn [idacs lang]
                        (update-in idacs [lang :defs]
                                   (fn [defs]
                                     (update-defs idacs config-dir cache-dir lang defs))))
                      idacs
                      [:clj :cljs :cljc])
        _ (doseq [[clazz mems] (:java-member-definitions idacs)]
            (to-cache config-dir cache-dir "java" clazz mems))
        idacs (let [jcu (:java-class-usages idacs)
                    classes-to-load (distinct (map :class jcu))]
                (reduce (fn [idacs class-to-load]
                          (if-not (get-in idacs [:java-member-definitions class-to-load])
                            (if-let [clazz-data (from-cache-1 cache-dir "java" class-to-load)]
                              (assoc-in idacs [:java-member-definitions class-to-load] clazz-data)
                              idacs)
                            idacs))
                        idacs
                        classes-to-load))]
    idacs))

(defn sync-cache [idacs config-dir cache-dir]
  (if cache-dir
    (with-thread-lock
      (with-cache cache-dir 6
        (sync-cache* idacs config-dir cache-dir)))
    (sync-cache* idacs config-dir cache-dir)))
;;;; Spec index
;;
;; A project-wide index of spec registrations (`s/def`/`s/fdef`), mirroring
;; spec's own global registry. Used by the :redefined-spec linter to detect
;; redefinitions across separate runs, independent of the require graph.
;;
;; The index is sharded, next to the per-namespace cache entries, under
;; `<cache-dir>/specs`:
;;
;; - `keys/<bucket>.transit.json` holds, for a bucket of spec identities, the
;;   registrations of that spec per source file. Buckets are addressed by a
;;   digest of the spec identity, so a run only touches the buckets for the
;;   specs it actually registers instead of reading and rewriting a
;;   project-wide blob.
;; - `files/<digest-of-path>.transit.json` is a per-file manifest of the spec
;;   identities that file contributed last time, so re-linting it can drop the
;;   registrations it no longer has.
;;
;; Shards are written by atomic rename and every run is the single writer for
;; its own files, so no global cache lock is needed.

(defn- spec-index-dir [cache-dir]
  (fs/path cache-dir "specs"))

(defn- digest
  "Stable, filesystem-safe name for a string."
  ^String [^String s]
  (let [md (java.security.MessageDigest/getInstance "SHA-1")
        sb (StringBuilder.)]
    (doseq [b (.digest md (.getBytes s "UTF-8"))]
      (.append sb (format "%02x" (bit-and (int b) 0xff))))
    (str sb)))

(defn- manifest-file [cache-dir canonical-path]
  (fs/path (spec-index-dir cache-dir) "files"
           (str (digest canonical-path) ".transit.json")))

(defn- spec-key
  "The identity a registration is indexed under, matching the grouping used by
  the :redefined-spec linter."
  [occ]
  [(:kind occ) (:ns occ) (:name occ) (:lang occ)])

(defn- bucket-file [cache-dir spec-key]
  ;; one hex byte of the digest: enough buckets to keep them small, few enough
  ;; that a full-project run doesn't write thousands of tiny files
  (fs/path (spec-index-dir cache-dir) "keys"
           (str (subs (digest (pr-str spec-key)) 0 2) ".transit.json")))

(defn- read-shard
  "Reads one shard, or nil if it's absent or unreadable (e.g. concurrently
  replaced or written by an incompatible version)."
  [f]
  (when (fs/exists? f)
    (try (let [data (with-open [is (io/input-stream (fs/file f))]
                      (transit/read (transit/reader is :json)))]
           (when (map? data) data))
         (catch Exception _ nil))))

(defn- write-shard!
  "Writes a shard via a temp file + atomic rename, so concurrent readers see
  either the old or the new contents, never a partial write."
  [f data]
  (let [dir (fs/parent f)
        _ (fs/create-dirs dir)
        tmp (fs/create-temp-file {:dir dir
                                  :prefix "spec-shard"
                                  :suffix ".transit.json"})]
    (try
      (with-open [os (no-flush-output-stream (io/output-stream (fs/file tmp)))]
        (transit/write (transit/writer os :json) data))
      (try (fs/move tmp f {:replace-existing true :atomic-move true})
           (catch java.nio.file.AtomicMoveNotSupportedException _
             (fs/move tmp f {:replace-existing true})))
      (finally (fs/delete-if-exists tmp)))))

(defn- delete-quietly! [f]
  (try (fs/delete-if-exists f) (catch Exception _ false)))

(defn- current-registrations
  "The registrations of this run, as spec-key -> canonical file -> entries. Only
  location and the filename spelling used in this run are stored; the rest of the
  identity is the key itself."
  [current-contributions canonical-by-filename]
  (reduce-kv (fn [m filename occs]
               (let [canonical (canonical-by-filename filename)]
                 (reduce (fn [m occ]
                           (update-in m [(spec-key occ) canonical] (fnil conj [])
                                      {:filename filename
                                       :row (:row occ)
                                       :col (:col occ)}))
                         m
                         occs)))
             {}
             current-contributions))

(defn- registered-keys-by-file
  "Inverts the registrations into canonical file -> the spec identities it
  registers now, which is what each file's manifest records."
  [registrations]
  (reduce-kv (fn [m k by-file]
               (reduce-kv (fn [m f _entries] (update m f (fnil conj []) k))
                          m
                          by-file))
             {}
             registrations))

(defn- previous-spec-keys
  "Reads the manifests of the files linted this run: canonical file -> the spec
  identities it registered according to the previous run. Their buckets have to
  be visited too, to drop registrations that are gone now."
  [cache-dir canonical-paths]
  (into {}
        (map (juxt identity #(:keys (read-shard (manifest-file cache-dir %)))))
        canonical-paths))

(defn- external-occurrences
  "Rebuilds full registration maps from a spec identity and its per-file entries,
  marked as non-reportable so they only serve as `first defined at` originals."
  [[kind ns name lang] by-file]
  (map #(assoc % :kind kind :ns ns :name name :lang lang :reportable? false)
       (mapcat val by-file)))

(defn- update-bucket
  "Applies this run's registrations to one bucket and returns
  `[updated-index external-occurrences]`, where the external occurrences are the
  registrations of the bucket's keys coming from files not linted this run."
  [index bucket-keys registrations external-file?]
  (reduce
   (fn [[index externals] k]
     ;; keep only what files outside this run still register: this run's own
     ;; entries are superseded by `registrations`, and entries of vanished
     ;; files must not be reported as the original definition
     (let [kept (into {} (filter (comp external-file? key)) (get index k))
           by-file (merge kept (get registrations k))]
       [(if (seq by-file) (assoc index k by-file) (dissoc index k))
        (into externals (external-occurrences k kept))]))
   [index []]
   bucket-keys))

(defn- sync-buckets!
  "Rewrites every bucket affected by this run and returns the external
  registrations found in them."
  [cache-dir affected-keys registrations external-file?]
  (reduce-kv (fn [acc f bucket-keys]
               (let [index (or (:index (read-shard f)) {})
                     [index' externals] (update-bucket index bucket-keys
                                                       registrations external-file?)]
                 (when-not (= index index')
                   (if (seq index')
                     (write-shard! f {:index index'})
                     (delete-quietly! f)))
                 (into acc externals)))
             []
             (group-by #(bucket-file cache-dir %) affected-keys)))

(defn- write-manifests!
  "Records what each file contributes now, so a later run can drop it again.
  Manifests that didn't change are left alone."
  [cache-dir canonical-paths current-keys previous-keys]
  (doseq [canonical canonical-paths
          :let [ks (get current-keys canonical)]
          :when (not= ks (get previous-keys canonical))
          :let [f (manifest-file cache-dir canonical)]]
    (if (seq ks)
      (write-shard! f {:file canonical :keys ks})
      (delete-quietly! f))))

(defn sync-spec-index!
  "Refreshes the spec index for the files linted in this run and returns the
  registrations of the same specs contributed by *other* files (as
  `first defined at` originals for cross-run detection).

  `current-contributions` is a map of filename -> vector of registration maps.
  `current-filenames` is the set of files linted this run; the registrations
  they no longer have are dropped from the index, so removing an `s/def` also
  removes it."
  [cache-dir current-contributions current-filenames]
  (when cache-dir
    (let [canonical-by-filename (into {}
                                      (map (juxt identity (comp str fs/canonicalize)))
                                      (into (set current-filenames)
                                            (keys current-contributions)))
          current-paths (set (vals canonical-by-filename))
          registrations (current-registrations current-contributions canonical-by-filename)
          current-keys (registered-keys-by-file registrations)
          previous-keys (previous-spec-keys cache-dir current-paths)
          external-file? (fn [f] (and (not (contains? current-paths f))
                                      (fs/exists? f)))
          ;; only the buckets of specs registered now or previously by these
          ;; files can be affected by this run
          affected-keys (distinct (concat (keys registrations)
                                          (mapcat val previous-keys)))
          externals (sync-buckets! cache-dir affected-keys registrations external-file?)]
      (write-manifests! cache-dir current-paths current-keys previous-keys)
      externals)))

;;;; Scratch

(comment
  (from-cache-1 nil :clj 'clojure.datafy)
  (get-in (from-cache-1 nil :cljc 'cljs.core) [:cljs 'defn-])
  (get-in (from-cache-1 nil :cljc 'cljs.core) [:cljs 'when-assert])
  (get-in (from-cache-1 nil :clj 'clojure.core) ['defn])
  (time (get (from-cache-1 nil :clj 'clojure.core) '+))
  (time (get (from-cache-1 nil :clj 'java.lang.Thread) 'sleep))

  (get (from-cache-1 nil :clj 'clojure.core) 'agent-errors)
  (from-cache-1 nil :clj 'clojure.core.specs.alpha))
