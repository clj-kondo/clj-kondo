(ns clj-kondo.impl.cache
  {:no-doc true}
  (:require
   [clj-kondo.impl.output :as output]
   [clj-kondo.impl.types.utils :as tu]
   [clj-kondo.impl.utils :refer [one-of]]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [cognitect.transit :as transit])
  (:import [java.io RandomAccessFile]))

(set! *warn-on-reflection* true)

(defn built-in-cache-resource [lang ns-sym]
  (io/resource (str "clj_kondo/impl/cache/built_in/"
                    (name lang) "/" (str ns-sym ".transit.json"))))

(defn cache-file ^java.io.File [cache-dir lang ns-sym]
  (io/file cache-dir (name lang) (str ns-sym ".transit.json")))

(defn from-cache-1 [cache-dir lang ns-sym]
  (when-let [{:keys [:resource :source]}
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
                   (binding [*out* output/err]
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
                      ns-data
                      (if resolve?
                        (if (identical? lang :cljc)
                          (-> ns-data
                              (update :clj #(tu/resolve-return-types idacs %))
                              (update :cljs #(tu/resolve-return-types idacs %)))
                          (tu/resolve-return-types idacs ns-data))
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
                [:clj :cljs :cljc])]
    (reduce (fn [idacs lang]
              (update-in idacs [lang :defs]
                         (fn [defs]
                           (update-defs idacs config-dir cache-dir lang defs))))
            idacs
            [:clj :cljs :cljc])))

(defn sync-cache [idacs config-dir cache-dir]
  (if cache-dir
    (with-cache cache-dir 6
      (sync-cache* idacs config-dir cache-dir))
    (sync-cache* idacs config-dir cache-dir)))

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
