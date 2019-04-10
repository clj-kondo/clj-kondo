(ns clj-kondo.impl.cache
  {:no-doc true}
  (:require [clj-kondo.impl.cache.clojure.core :as cache-clojure-core]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [cognitect.transit :as transit])
  (:import [java.io RandomAccessFile]
           [java.nio.channels FileChannel]))

(set! *warn-on-reflection* true)

(defn cache-file ^java.io.File [cache-dir lang ns-sym]
  (io/file cache-dir (name lang) (str ns-sym ".transit.json")))

(defn from-cache-1 [cache-dir lang ns-sym]
  (let [file (cache-file cache-dir lang ns-sym)]
    (when (.exists file)
      (transit/read (transit/reader
                     (io/input-stream file) :json)))))

(defn to-cache
  "Writes ns-data to cache-dir. Always use with `with-cache`."
  [cache-dir lang ns-sym ns-data]
  (let [file (cache-file cache-dir lang ns-sym)
        ;; first we write to a baos as a workaround for transit-clj #43
        bos (java.io.ByteArrayOutputStream. 1024)
        writer (transit/writer (io/output-stream bos) :json)]
    (io/make-parents file)
    (transit/write writer ns-data)
    (io/copy (.toByteArray bos) file)))

(defmacro with-cache
  "Tries to lock cache in the scope of `body`. Retries `max-retries`
  times while sleeping 250ms in between. If not succeeded after
  retries, throws `Exception`."
  [cache-dir max-retries & body]
  `(let [lock-file# (io/file ~cache-dir "lock")
         _# (io/make-parents lock-file#)
         raf# (RandomAccessFile. lock-file# "rw")
         channel# (.getChannel raf#)]
     (loop [retry# 0]
       (if-let [lock#
                (try (.tryLock channel#)
                     (catch java.nio.channels.OverlappingFileLockException _#
                       nil))]
         (try
           ~@body
           (finally (.release ^java.nio.channels.FileLock lock#)))
         (do
           (Thread/sleep 250)
           (if (= retry# ~max-retries)
             (throw (Exception.
                     (str "clj-kondo cache is locked by other process")))
             (recur (inc retry#))))))))

(defn from-cache [cache-dir lang namespaces]
  (reduce (fn [acc ns-sym]
            (if-let [data (from-cache-1 cache-dir
                                        lang ns-sym)]
              (update acc ns-sym
                      (fn [ns]
                        (merge data ns)))
              acc))
          {} namespaces))

(defn sync-cache [idacs cache-dir]
  (with-cache cache-dir 6
    (reduce (fn [idacs lang]
              (let [analyzed-namespaces
                    (set (keys (get-in idacs [lang :defns])))
                    called-namespaces
                    (conj (set (keys (get-in idacs [lang :calls])))
                          (case lang
                            :clj 'clojure.core
                            :cljs 'cljs.core
                            :cljc 'clojure.core))
                    load-from-cache
                    (set/difference called-namespaces analyzed-namespaces)
                    defns-from-cache
                    (from-cache cache-dir lang load-from-cache)
                    cljc-defns-from-cache
                    (from-cache cache-dir :cljc load-from-cache)]
                (doseq [ns-name analyzed-namespaces]
                  (let [ns-data (get-in idacs [lang :defns ns-name])]
                    (to-cache cache-dir lang ns-name ns-data)))
                (-> idacs
                    (update-in [lang :defns]
                               (fn [idacs]
                                 (merge defns-from-cache idacs)))
                    (update-in [:cljc :defns]
                               (fn [idacs]
                                 (merge cljc-defns-from-cache idacs))))))
            idacs
            [:clj :cljs :cljc])))

(def built-in-cache
  {'clojure.core cache-clojure-core/cache})

(defn with-built-ins
  "Enriches idacs with built-in var information."
  [idacs]
  (-> idacs
      (cond-> (not (get-in idacs '[:clj :defns clojure.core]))
        (assoc-in '[:clj :defns clojure.core] (get built-in-cache 'clojure.core)))))

;;;; Scratch

(comment
  (from-cache-1 (io/file ".clj-kondo" ".cache" "2019.03.29-alpha3-SNAPSHOT")
                :cljc 'corpus.cljc.test-cljc)
  (from-cache-1 (io/file ".clj-kondo" ".cache" "2019.03.29-alpha3-SNAPSHOT")
                :clj 'clojure.main)
  )
