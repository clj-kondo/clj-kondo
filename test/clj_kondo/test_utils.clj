(ns clj-kondo.test-utils
  (:require
   [clj-kondo.main :as main :refer [main]]
   [clojure.string :as str :refer [trim]]
   [me.raynes.conch :refer [programs with-programs let-programs] :as sh]
   [clojure.java.io :as io]))

(defn parse-output [msg]
  (map (fn [[_ file row col level message]]
         {:file file
          :row (Integer/parseInt row)
          :col (Integer/parseInt col)
          :level (keyword level)
          :message message})
       (keep
        #(re-matches #"(.*):(.*):(.*): (.*): (.*)" %)
        (str/split-lines msg))))

(defn lint-jvm!
  ([input] (lint-jvm! input "--lang" "clj"))
  ([input & args]
   (let [res (if (instance? java.io.File input)
               (with-out-str
                 (apply main "--lint" (.getPath input) args))
               (with-out-str
                 (with-in-str input
                   (apply main "--lint" "-" args))))]
     #_(println res)
     (parse-output res))))

(defn lint-native!
  ([input] (lint-native! input "--lang" "clj"))
  ([input & args]
   (let [res (let-programs [clj-kondo "./clj-kondo"]
               (binding [sh/*throw* false]
                 (if (instance? java.io.File input)
                   (apply clj-kondo "--lint" (.getPath input) args)
                   (apply clj-kondo  "--lint" "-" (conj (vec args)
                                                        ;; the opts go last
                                                        {:in input})))))]
     (parse-output res))))

(def lint!
  (case (System/getenv "CLJ_KONDO_TEST_ENV")
    "jvm" lint-jvm!
    "native" lint-native!
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
