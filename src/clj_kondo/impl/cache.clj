(ns clj-kondo.impl.cache
  {:no-doc true}
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [cognitect.transit :as transit])
  (:import [java.io RandomAccessFile]
           [java.nio.channels FileChannel]))

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
                 {:source :built-in
                  :resource (built-in-cache-resource lang ns-sym)})]
    (when resource
      (assoc
       (transit/read (transit/reader
                      (io/input-stream resource) :json))
       :source source))))

(defn from-cache [cache-dir lang namespaces]
  (reduce (fn [acc ns-sym]
            (if-let [data (from-cache-1 cache-dir
                                        lang ns-sym)]
              (update acc ns-sym
                      (fn [ns]
                        (merge data ns)))
              acc))
          {} namespaces))

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

(defn clojure-core [cache-dir]
  (from-cache-1 cache-dir :clj 'clojure.core))

(defn cljs-core [cache-dir]
  (from-cache-1 cache-dir :cljs 'cljs.core))

(defn sync-cache* [idacs cache-dir]
  (reduce (fn [idacs lang]
            (let [analyzed-namespaces
                  (set (keys (get-in idacs [lang :defs])))
                  called-namespaces
                  (set (keys (get-in idacs [lang :calls])))
                  load-from-cache
                  (set/difference called-namespaces analyzed-namespaces
                                  ;; clojure core is loaded later
                                  '#{clojure.core cljs.core})
                  defs-from-cache
                  (from-cache cache-dir lang load-from-cache)
                  cljc-defs-from-cache
                  (from-cache cache-dir :cljc load-from-cache)]
              (when cache-dir
                (doseq [ns-name analyzed-namespaces
                        :let [{:keys [:source] :as ns-data}
                              (get-in idacs [lang :defs ns-name])]
                        :when (not (contains? #{:disk :built-in} source))]
                  (to-cache cache-dir lang ns-name ns-data)))
              (let [idacs (-> idacs
                             (update-in [lang :defs]
                                        (fn [idacs]
                                          (merge defs-from-cache idacs)))
                             (update-in [:cljc :defs]
                                        (fn [idacs]
                                          (merge cljc-defs-from-cache idacs))))]
                ;; load clojure.core and/or cljs.core only once
                (case lang
                  (:clj :cljc)
                  (if (and (seq called-namespaces)
                           (not (get-in idacs [:clj :defs 'clojure.core])))
                    (assoc-in idacs [:clj :defs 'clojure.core]
                              (clojure-core cache-dir))
                    idacs)
                  :cljs
                  (if (and (seq called-namespaces)
                           (not (get-in idacs [:cljs :defs 'cljs.core])))
                    (assoc-in idacs [:cljs :defs 'cljs.core]
                              (cljs-core cache-dir))
                    idacs)))))
          idacs
          [:clj :cljs :cljc]))

(defn sync-cache [idacs cache-dir]
  (if cache-dir
    (with-cache cache-dir 6
      (sync-cache* idacs cache-dir))
    (sync-cache* idacs cache-dir)))

;;;; Scratch

(comment
  (from-cache-1 nil :clj 'clojure.datafy)
  (get (from-cache-1 nil :cljs 'cljs.core) 'apply)
  (time (get (from-cache-1 nil :clj 'clojure.core) '+))
  (time (get (from-cache-1 nil :clj 'java.lang.Thread) 'sleep))
  )
