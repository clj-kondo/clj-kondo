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

#_(defn class->package
  "Implementation by Marco Marini."
  [class-file]
  (with-open [dis (java.io.DataInputStream. (io/input-stream class-file))]
    ;; skip first 8 bytes
    (.readLong dis)
    (let [constant-pool-count (dec (.readUnsignedShort dis))
          counter (volatile! 0)
          classes (volatile! {})
          strings (volatile! {})]
      (while (< @counter constant-pool-count)
        (do
          (case (.read dis)
            1 (vswap! strings assoc @counter (.readUTF dis))
            5 (do (.readLong dis) (vswap! counter inc))
            6 (do (.readLong dis) (vswap! counter inc))
            7 (vswap! classes assoc  @counter (.readShort dis))
            8 (.readShort dis)
            (.readInt dis))
          (swap! counter inc)))
      ;; skip access flags
      (.readShort dis)
      ;; (prn (get @classes (.readUnsignedShort dis)))
      (clojure.string/join
       "."
       (butlast
        (clojure.string/split
         (get @strings (- (get @classes (- (.readUnsignedShort dis) 1)) 1))
         #"/"))))))

(defn class->class-name [class-file]
  (let [bytes (file->bytes class-file)
        ;; we use ASM for reading the fully qualified class name
        ;; hand-made solutions, if we ever want to get rid of ASM:
        ;; https://stackoverflow.com/questions/1649674/resolve-class-name-from-bytecode/1650442#comment115293993_1650442
        ;; https://stackoverflow.com/a/52332101/6264
        ;; see class->package above
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

(defn reg-java-class-def! [ctx {:keys [jar entry file]}]
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
                          (str jar ":" entry))})))

(defn reg-java-class-usage! [ctx class-name loc]
  (swap! (:analysis ctx)
         update :java-class-usages conj
         (merge {:class class-name
                 :uri (:uri ctx)
                 :filename (:filename ctx)}
                loc))
  nil)
