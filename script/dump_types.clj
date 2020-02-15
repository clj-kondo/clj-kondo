#!/usr/bin/env bb

(ns dump-types
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]))

(def clj-files (rest (file-seq (io/file "resources" "clj_kondo" "impl" "cache" "built_in" "clj"))))

(defn transit->edn [f]
  (-> (sh/sh "jet" "--from" "transit" "--to" "--edn" :in f)
      :out
      edn/read-string))

(defn types [f]
  (let [edn (transit->edn f)]
    (for [[k v] edn
          :let [ar (:arities v)]
          :when ar]
      [k ar])))

(doseq [f clj-files]
  (println "=== " (.getPath f) " ===")
  (doseq [[k v] (types f)]
    (println k v))
  (println))

