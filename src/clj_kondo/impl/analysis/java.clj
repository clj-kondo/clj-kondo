(ns clj-kondo.impl.analysis.java
  (:require [clj-kondo.impl.utils :refer [->uri]]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (org.objectweb.asm ClassReader)))

(set! *warn-on-reflection* true)

(defn file->bytes [file]
  (with-open [xin (io/input-stream file)
              xout (java.io.ByteArrayOutputStream.)]
    (io/copy xin xout)
    (.toByteArray xout)))

(defn entry->class-name [entry]
  (-> entry
      (str/replace "/" ".")
      (str/replace "\\" ".")
      (str/replace ".class" "")
      (str/replace ".java" "")))

(defn class->class-name [class-file]
  (let [bytes (file->bytes class-file)
        ;; we use ASM for reading the fully qualified class name
        ;; hand-made solutions, if we ever want to get rid of ASM:
        ;; https://stackoverflow.com/questions/1649674/resolve-class-name-from-bytecode/1650442#comment115293993_1650442
        ;; https://stackoverflow.com/a/52332101/6264
        ;; Also see working example in Clojure https://gist.github.com/borkdude/d02dc3ff1d03d09351e768964983a46b
        rdr (new ClassReader ^bytes bytes)
        class-name (.getClassName rdr)
        class-name (str/replace class-name "/" ".")]
    class-name))

(defn source->class-name [file]
  (let [fname (entry->class-name file)
        fname (str/replace fname "\\" ".")
        class-name (last (str/split fname #"\."))]
    (binding [*in* (io/reader file)]
      (loop []
        (if-let [next-line (read-line)]
          (if-let [[_ package] (re-matches #"\s*package\s+(\S*)\s*;\s*" next-line)]
            (str package "." class-name)
            (recur))
          class-name)))))

(defn java-class-def-analysis? [ctx]
  (-> ctx :config ))

(defn analyze-class-defs? [ctx]
  (:analyze-java-class-defs? ctx))

(defn reg-class-def! [ctx {:keys [jar entry file]}]
  (when (analyze-class-defs? ctx)
    (when-let [class-name (if entry
                            (entry->class-name entry)
                            (when file
                              (cond
                                (str/ends-with? file ".class")
                                (class->class-name file)
                                (str/ends-with? file ".java")
                                (source->class-name file))))]
      (swap! (:analysis ctx)
             update :java-class-definitions conj
             {:class class-name
              :uri (->uri jar entry file)
              :filename (or file
                            (str jar ":" entry))}))))

(defn analyze-class-usages? [ctx]
  (and (:analyze-java-class-usages? ctx)
       (identical? :clj (:lang ctx))))

(defn reg-class-usage!
  [ctx class-name loc+data]
  (when (analyze-class-usages? ctx)
    (swap! (:analysis ctx)
           update :java-class-usages conj
           (merge {:class class-name
                   :uri (:uri ctx)
                   :filename (:filename ctx)}
                  loc+data)))
  nil)
