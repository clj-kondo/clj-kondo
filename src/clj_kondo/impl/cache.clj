(ns clj-kondo.impl.cache
  {:no-doc true}
  (:require
   [clj-kondo.impl.profiler :as profiler]
   [clj-kondo.impl.utils :refer [one-of]]
   [clojure.java.io :as io]
   [cognitect.transit :as transit])
  (:import [java.io RandomAccessFile]))

(set! *warn-on-reflection* true)

(defn built-in-cache-resource [lang ns-sym]
  (io/resource (str "clj_kondo/impl/cache/built_in/"
                    (name lang) "/" (str ns-sym ".transit.json"))
               ;; workaround for https://github.com/oracle/graal/issues/1287
               (.getClassLoader clojure.lang.RT)))

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
  (let [file (cache-file cache-dir lang ns-sym)]
    (with-open [;; first we write to a baos as a workaround for transit-clj #43
                bos (java.io.ByteArrayOutputStream. 1024)
                os (io/output-stream bos)]
      (let [writer (transit/writer os :json)]
        (io/make-parents file)
        (transit/write writer ns-data)
        (io/copy (.toByteArray bos) file)))))

(defmacro with-cache
  "Tries to lock cache in the scope of `body`. Retries `max-retries`
  times while sleeping 250ms in between. If not succeeded after
  retries, throws `Exception`."
  [cache-dir max-retries & body]
  `(let [lock-file# (io/file ~cache-dir "lock")]
     (io/make-parents lock-file#)
     (with-open [raf# (RandomAccessFile. lock-file# "rw")
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
                       (str "Clj-kondo cache is locked by other process.")))
               (recur (inc retry#)))))))))

(defn load-when-missing [idacs cache-dir lang ns-sym]
  (let [path [lang :defs ns-sym]]
    (if-not (get-in idacs path)
      (if-let [data (from-cache-1 cache-dir lang ns-sym)]
        (let [res (assoc-in idacs path data)]
          ;; proxied-namespaces are here because of potemkin/import-vars since
          ;; import-vars only supports clj and not cljs, we're fine with loading
          ;; these namespace only with the current language (which is :clj)
          (if-let [proxied (:proxied-namespaces data)]
            (reduce #(load-when-missing %1 cache-dir lang %2) res proxied)
            res))
        idacs)
      idacs)))

(defn sync-cache* [idacs cache-dir]
  (reduce (fn [idacs lang]
            (let [required-namespaces (get-in idacs [lang :used-namespaces])
                  analyzed-namespaces
                  (set (keys (get-in idacs [lang :defs])))]
              (when cache-dir
                (doseq [ns-name analyzed-namespaces
                        :let [{:keys [:source] :as ns-data}
                              (get-in idacs [lang :defs ns-name])]
                        :when (and (not (one-of source [:disk :built-in]))
                                   (seq ns-data))]
                  (to-cache cache-dir lang ns-name ns-data)))
              (reduce (fn [idacs lang]
                        (reduce #(load-when-missing %1 cache-dir lang %2)
                                idacs
                                required-namespaces))
                      idacs
                      (case lang
                        (:cljs :cljc) [:clj :cljs :cljc]
                        :clj [:clj :cljc]))))
          idacs
          [:clj :cljs :cljc]))

(defn sync-cache [idacs cache-dir]
  (profiler/profile
   :sync-cache
   (if cache-dir
     (with-cache cache-dir 6
       (sync-cache* idacs cache-dir))
     (sync-cache* idacs cache-dir))))

;;;; Scratch

(comment
  (from-cache-1 nil :clj 'clojure.datafy)
  (get-in (from-cache-1 nil :cljc 'cljs.core) [:cljs 'defn-])
  (get-in (from-cache-1 nil :cljc 'cljs.core) [:cljs 'when-assert])
  (get-in (from-cache-1 nil :clj 'clojure.core) ['defn])
  (time (get (from-cache-1 nil :clj 'clojure.core) '+))
  (time (get (from-cache-1 nil :clj 'java.lang.Thread) 'sleep))

  (get (from-cache-1 nil :clj 'clojure.core) 'agent-errors)
  (from-cache-1 nil :clj 'clojure.core.specs.alpha)
  )
