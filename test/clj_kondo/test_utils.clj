(ns clj-kondo.test-utils
  (:require
   [clj-kondo.main :as main :refer [main]]
   [clojure.string :as str :refer [trim]]
   [clojure.test :refer [is]]
   [me.raynes.conch :refer [programs with-programs let-programs] :as sh]))

(defn submap?
  "Is m1 a subset of m2? Taken from
  https://github.com/clojure/spec-alpha2, clojure.test-clojure.spec"
  [m1 m2]
  (if (and (map? m1) (map? m2))
    (every? (fn [[k v]] (and (contains? m2 k)
                             (submap? v (get m2 k))))
            m1)
    (= m1 m2)))

(defmacro assert-submap [m r]
  `(is (submap? ~m ~r)))

(defmacro assert-some-submap [m r]
  `(is (some #(submap? ~m %) ~r)))

(defmacro assert-submaps
  "Asserts that maps are submaps of result in corresponding order and
  that the number of maps corresponds to the number of
  results. Returns true if all assertions passed (useful for REPL)."
  [maps result]
  `(and
    (is (= (count ~maps) (count ~result)))
    (every? identity
            (for [[m# r#] (map vector ~maps ~result)]
              (assert-submap m# r#)))))

(defn parse-output
  "Parses linting output and prints everything that doesn't match the
  expected format (for debugging)."
  [msg]
  (doall
   (keep
    (fn [line]
      (if-let [[_ file row col level message] (re-matches #"(.*):(.*):(.*): (.*): (.*)" line)]
        {:file file
         :row (Integer/parseInt row)
         :col (Integer/parseInt col)
         :level (keyword level)
         :message message}
        (when-not (str/starts-with? line "linting took")
          (println line))))
    (str/split-lines msg))))

(defn lint-jvm!
  ([input]
   (require '[clj-kondo.impl.config] :reload)
   (lint-jvm! input "--lang" "clj"))
  ([input & args]
   (require '[clj-kondo.impl.config] :reload)
   (let [res (with-out-str
               (try
                 (cond
                   (instance? java.io.File input)
                   (apply main "--lint" (.getPath input) args)
                   (vector? input)
                   (apply main "--lint" (concat (map #(.getPath %) input) args))
                   :else (with-in-str input
                           (apply main "--lint" "-" args)))
                 (catch Throwable e
                   (.printStackTrace e))))]
     (parse-output res))))

(defn lint-native!
  ([input] (lint-native! input "--lang" "clj"))
  ([input & args]
   (let [res (let-programs [clj-kondo "./clj-kondo"]
               (binding [sh/*throw* false]
                 (cond
                   (instance? java.io.File input)
                   (apply clj-kondo "--lint" (.getPath input) args)
                   (vector? input)
                   (apply clj-kondo "--lint" (concat (map #(.getPath %) input) args))
                   :else
                   (apply clj-kondo  "--lint" "-" (conj (vec args)
                                                        ;; the opts go last
                                                        {:in input})))))]
     (parse-output res))))

(def lint!
  (case (System/getenv "CLJ_KONDO_TEST_ENV")
    "jvm" #'lint-jvm!
    "native" #'lint-native!
    lint-jvm!))

(if (= lint! lint-jvm!)
  (println "==== Testing JVM version")
  (println "==== Testing native version"))

;;;; Scratch

(comment
  (let-programs [clj-kondo "./clj-kondo"]
    (apply clj-kondo "--cache" ["--lint" "-" {:in "(defn foo [x] x) (foo 1 2 3)"}]))

  (lint-native! "(defn foo [x] x) (foo 1 2 3)")
  (lint-native! (io/file "test"))
  )
