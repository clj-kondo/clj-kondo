(ns clj-kondo.test-utils
  (:require
   [clj-kondo.impl.config :as conf]
   [clj-kondo.impl.hooks :as hooks]
   [clj-kondo.impl.utils :refer [deep-merge]]
   [clj-kondo.main :as main :refer [main]]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :as t :refer [*report-counters* deftest is testing]]
   [clojure.walk :as walk]
   [matcher-combinators.test]
   [me.raynes.conch :refer [let-programs programs] :as sh]))

(set! *warn-on-reflection* true)

(defmethod t/report :begin-test-var [m]
  (println "===" (-> m :var meta :name))
  (println))

(defmethod t/report :end-test-var [_m]
  (when-let [rc *report-counters*]
    (when-let [{:keys [:fail :error]} @rc]
      (when (and (= "true" (System/getenv "CLJ_KONDO_FAIL_FAST"))
                 (or (pos? fail) (pos? error)))
        (println "=== Failing fast")
        (System/exit 1)))))

(defn normalize-filename [s]
  (str/replace s "\\" "/"))

(defn regex? [x]
  (instance? java.util.regex.Pattern x))

(defn submap?
  "Is m1 a subset of m2? Taken from
  https://github.com/clojure/spec-alpha2, clojure.test-clojure.spec"
  [m1 m2]
  (cond
    (and (map? m1) (map? m2))
    (every? (fn [[k v]]
              (if (identical? v :submap/missing)
                (not (contains? m2 k))
                (and (contains? m2 k)
                     (if (or (identical? k :filename)
                             (identical? k :file))
                       (if (regex? v)
                         (re-find v (normalize-filename (get m2 k)))
                         (= (normalize-filename v)
                            (normalize-filename (get m2 k))))
                       (submap? v (get m2 k))))))
            m1)
    (regex? m1)
    (re-find m1 m2)
    :else (= m1 m2)))

(defmacro assert-submap [m r]
  `(is (submap? ~m ~r)))

(defmacro assert-submaps
  "Asserts that maps are submaps of result in corresponding order and
  that the number of maps corresponds to the number of
  results. Returns true if all assertions passed (useful for REPL)."
  [maps result]
  `(let [maps# ~maps
         res# ~result]
     (and
      (is (= (count maps#) (count res#))
          (format "Expected %s results, but got: %s\n\n%s\n%s"
                  (count maps#) (count res#)
                  maps# (vec res#)))
      (doseq [m# maps#]
        (is (some #(submap? m# %) res#) (str "No superset of " m# " found"))))))

(defn normalize-map-paths
  "Any value with a key of `:file` or `:filename` is assumed to be a string path.
  NOTE: we don't walk map `m`, we only look at root key-values."
  [m]
  (reduce (fn [m k]
            (if (k m)
              (update m k (fn [v]
                            (if (regex? v)
                              v
                              (normalize-filename v))))
              m))
          m
          [:file :filename]))

(defmacro assert-submap2
  "A new version of assert-submap that uses nubank's matcher-combinators.
  Matcher-combinators does a nice job of highlighting the difference in actual vs expected."
  [expected actual]
  `(let [actual# ~actual
         expected# ~expected]
     (is (~'match? (normalize-map-paths expected#) (normalize-map-paths actual#)))))

(defmacro assert-submaps2
  "A new version of assert-submaps that uses nubank's matcher-combinators.
  Matcher-combinators does a nice job of highlighting the difference in actual vs expected."
  [expected actual]
  `(let [actual# ~actual
         expected# ~expected]
     (is (~'match? (map normalize-map-paths expected#) (map normalize-map-paths actual#)))))

(defn parse-output
  "Parses linting output and prints everything that doesn't match the
  expected format (for debugging)."
  [msg]
  (doall
   (keep
    (fn [line]
      (if-let [[_ file row col level message] (re-matches #"(.+):(\d+):(\d+): (\w+): (.*)" line)]
        {:file file
         :row (Integer/parseInt row)
         :col (Integer/parseInt col)
         :level (keyword level)
         :message message}
        (when-not (str/starts-with? line "linting took")
          (println line))))
    (str/split-lines msg))))

(def base-config
  {:output {:langs false}
   :linters {:unused-binding {:level :off}
             :unresolved-symbol {:level :off}
             :refer-all {:level :off}
             :type-mismatch {:level :off}
             :unsorted-required-namespaces {:level :off}
             :shadowed-var {:level :off}
             :loop-without-recur {:level :off}
             :redundant-fn-wrapper {:level :off}
             :namespace-name-mismatch {:level :off}
             :line-length {:level :off}
             :unused-value {:level :off}
             :uninitialized-var {:level :off}
             :redundant-str-call {:level :off}
             :redundant-ignore {:level :off}}})

(defn lint-jvm!
  ([input]
   (lint-jvm! input "--lang" "clj"))
  ([input & args]
   (hooks/reset-ctx!)
   (let [[config args]
         (let [m (first args)]
           (if (map? m)
             [m (rest args)]
             [nil args]))
         config (str (deep-merge conf/default-config base-config config))
         res (with-out-str
               (cond
                 (instance? java.io.File input)
                 (apply main "--cache" "false" "--lint" (.getPath ^java.io.File input) "--config" config args)
                 (vector? input)
                 (apply main "--cache" "false" "--lint" (concat (map #(.getPath ^java.io.File %) input)
                                                                ["--config" config] args))
                 :else (with-in-str input
                         (apply main "--cache" "false" "--lint" "-"  "--config" config args))))]
     ;; (println input)
     ;; (println res)
     (parse-output res))))

(def windows? (-> (System/getProperty "os.name")
                  (str/lower-case)
                  (str/includes? "win")))

(defn lint-native!
  ([input] (lint-native! input "--lang" "clj"))
  ([input & args]
   (let [[config args]
         (let [m (first args)]
           (if (map? m)
             [m (rest args)]
             [nil args]))
         config (str (deep-merge conf/default-config base-config config))
         config (if windows? (str/replace config "\"" "\\\"")
                    config)
         res (let-programs [clj-kondo "./clj-kondo"]
               (binding [sh/*throw* false]
                 (cond
                   (instance? java.io.File input)
                   (apply clj-kondo
                          "--cache" "false"
                          "--lint" (.getPath ^java.io.File input) "--config" config args)
                   (vector? input)
                   (apply clj-kondo
                          "--cache" "false"
                          "--lint" (concat (map #(.getPath ^java.io.File %) input)
                                           ["--config" config] args))
                   :else
                   (apply clj-kondo
                          "--cache" "false"
                          "--lint" "-" "--config" config
                          (conj (vec args)
                                ;; the opts go last
                                {:in input})))))]
     (parse-output res))))

(def native?
  (= "native" (System/getenv "CLJ_KONDO_TEST_ENV")))

(def lint!
  (if native? #'lint-native! #'lint-jvm!))

(if native?
  (println "==== Testing native version")
  (println "==== Testing JVM version" (clojure-version)))

(defn file-path
  "returns a file-path with platform specific file separator"
  [& more]
  (.getPath ^java.io.File (apply io/file more)))

(def file-separator java.io.File/separator)

(programs rm mkdir mv)

;; https://gist.github.com/olieidel/c551a911a4798312e4ef42a584677397
(defn delete-directory-recursive
  "Recursively delete a directory."
  [^java.io.File file]
  ;; when `file` is a directory, list its entries and call this
  ;; function with each entry. can't `recur` here as it's not a tail
  ;; position, sadly. could cause a stack overflow for many entries?
  (when (.isDirectory file)
    (doseq [file-in-dir (.listFiles file)]
      (delete-directory-recursive file-in-dir)))
  ;; delete the file or directory. if it it's a file, it's easily
  ;; deletable. if it's a directory, we already have deleted all its
  ;; contents with the code above (remember?)
  (.delete file))

(defn remove-dir [dir]
  (let [f (io/file dir)]
    (when (.exists f)
      (if windows?
        (delete-directory-recursive f)
        (rm "-rf" dir)))))

(defn make-dirs [dir]
  (if windows?
    (.mkdirs (io/file dir))
    (mkdir "-p" dir)))

(defn rename-path [old-path new-path]
  (if windows?
    (.renameTo (io/file old-path) (io/file new-path))
    (mv old-path new-path)))

(defmacro with-temp-dir
  [[binding dir-name] & body]
  `(let [tmp-dir#  (System/getProperty "java.io.tmpdir")
         test-dir# (.getPath (io/file tmp-dir# ~dir-name))
         ~binding  test-dir#]
     (remove-dir test-dir#)
     (make-dirs test-dir#)
     (try
       ~@body
       (finally
         (remove-dir test-dir#)))))

(defn template [expr replacement-map]
  (walk/postwalk-replace replacement-map expr))

(defn normalize-newlines [s]
  (if (and s windows?)
    (str/replace s "\r\n" "\n")
    s))

;;;; Scratch

(comment
  (template '{:linters {:unresolved-symbol {:exclude [::foo y z]}}}
            '{::foo x})
  )
