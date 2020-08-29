#!/usr/bin/env bb

(ns dump-types
  (:require
   [clojure.java.io :as io]
   [cognitect.transit :as transit]))

(def clj-files (rest (file-seq (io/file "resources" "clj_kondo" "impl" "cache" "built_in" "clj"))))

(defn transit->edn [f]
  (with-open [is (io/input-stream (io/file f))]
    (let [reader (transit/reader is :json)]
      (transit/read reader))))

(defn types [f]
  (let [edn (transit->edn f)]
    (for [[k v] edn
          :let [ar (:arities v)]
          :when ar]
      [k ar])))

(let [output
      (with-out-str
        (doseq [f clj-files]
          (println "=== " (.getPath f) " ===")
          (doseq [[k v] (types f)]
            (println k v))
          (println)))]
  (spit (io/file "doc" "types.txt") output))
