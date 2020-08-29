(ns clj-kondo.test-utils
  (:require
   [clj-kondo.impl.utils :refer [deep-merge]]
   [clj-kondo.main :as main :refer [main]]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [is]]
   [me.raynes.conch :refer [let-programs programs] :as sh]))

(set! *warn-on-reflection* true)

(defn normalize-filename [s]
  (str/replace s "\\" "/"))

(defn submap?
  "Is m1 a subset of m2? Taken from
  https://github.com/clojure/spec-alpha2, clojure.test-clojure.spec"
  [m1 m2]
  (cond
    (and (map? m1) (map? m2))
    (every? (fn [[k v]] (and (contains? m2 k)
                             (if (or (identical? k :filename)
                                     (identical? k :file))
                               (= (normalize-filename v)
                                  (normalize-filename (get m2 k)))
                               (submap? v (get m2 k)))))
            m1)
    (instance? java.util.regex.Pattern m1)
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
          (format "Expected %s results, but got: %s"
                  (count maps#) (count res#)))
      (doseq [m# maps#]
        (is (some #(submap? m# %) res#) (str "No superset of " m# " found"))))))

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
  '{:linters {:unused-binding {:level :off}
              :unresolved-symbol {:level :off}
              :refer-all {:level :off}
              :type-mismatch {:level :off}
              :unsorted-required-namespaces {:level :off}}})

(defn lint-jvm!
  ([input]
   (lint-jvm! input "--lang" "clj"))
  ([input & args]
   (let [[config args]
         (let [m (first args)]
           (if (map? m)
             [m (rest args)]
             [nil args]))
         config (str (deep-merge base-config config))
         res (with-out-str
               (try
                 (cond
                   (instance? java.io.File input)
                   (apply main "--cache" "false" "--lint" (.getPath ^java.io.File input) "--config" config args)
                   (vector? input)
                   (apply main "--cache" "false" "--lint" (concat (map #(.getPath ^java.io.File %) input)
                                                                  ["--config" config] args))
                   :else (with-in-str input
                           (apply main "--cache" "false" "--lint" "-"  "--config" config args)))
                 (catch Throwable e
                   (.printStackTrace e))))]
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
         config (str (deep-merge base-config config))
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

(defn rename-path [old new]
  (if windows?
    (.renameTo (io/file old) (io/file new))
    (mv old new)))

;;;; Scratch

(comment
  (let-programs [clj-kondo "./clj-kondo"]
    (apply clj-kondo "--cache" ["--lint" "-" {:in "(defn foo [x] x) (foo 1 2 3)"}]))

  (lint-native! "(defn foo [x] x) (foo 1 2 3)")
  (lint-native! (io/file "test"))
  )
