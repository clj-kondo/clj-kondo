(ns clj-kondo.impl.cache
  {:no-doc true}
  (:require [cognitect.transit :as transit]
            [clojure.java.io :as io])
  (:import [java.nio.channels FileChannel]
           [java.io RandomAccessFile]))

(set! *warn-on-reflection* true)

(defn cache-file ^java.io.File [cache-dir lang ns-sym]
  (io/file cache-dir (name lang) (str ns-sym ".transit.json")))

(defn from-cache [cache-dir lang ns-sym]
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

;;;; Scratch

(comment
  )
