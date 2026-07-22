(ns clj-kondo.method-size-test
  "Guards against methods crossing HotSpot's 8000-bytecode HugeMethodLimit.
  Methods over the limit are silently never JIT-compiled and run interpreted,
  which cost clj-kondo ~40% lint time before the 2026-07 method splits. The
  cliff is invisible at runtime, so this test AOT-compiles the hot namespaces
  and reads every method's code length straight from the class files."
  (:require
   [babashka.fs :as fs]
   [clojure.java.io :as io]
   [clojure.java.shell :refer [sh]]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]])
  (:import
   [java.io DataInputStream]))

(def ^:private huge-method-limit 8000)

(defn- read-utf8-pool
  "Reads the constant pool, returning {index string} for Utf8 entries and
  leaving `in` positioned right after the pool."
  [^DataInputStream in]
  (let [cp-count (.readUnsignedShort in)]
    (loop [idx 1 utf8 {}]
      (if (>= idx cp-count)
        utf8
        (let [tag (.readUnsignedByte in)]
          (case tag
            1 (let [len (.readUnsignedShort in)
                    bytes (byte-array len)]
                (.readFully in bytes)
                (recur (inc idx) (assoc utf8 idx (String. bytes "UTF-8"))))
            (7 8 16 19 20) (do (.skipBytes in 2) (recur (inc idx) utf8))
            15 (do (.skipBytes in 3) (recur (inc idx) utf8))
            (3 4 9 10 11 12 17 18) (do (.skipBytes in 4) (recur (inc idx) utf8))
            ;; longs and doubles take two pool slots
            (5 6) (do (.skipBytes in 8) (recur (+ idx 2) utf8))))))))

(defn- skip-attributes [^DataInputStream in]
  (dotimes [_ (.readUnsignedShort in)]
    (.skipBytes in 2)
    (.skipBytes in (.readInt in))))

(defn- method-code-sizes
  "Returns [[method-name code-length] ...] for every method in the class file."
  [f]
  (with-open [in (DataInputStream. (io/input-stream (fs/file f)))]
    (.skipBytes in 8) ;; magic + minor + major
    (let [utf8 (read-utf8-pool in)]
      (.skipBytes in 6) ;; access + this + super
      (.skipBytes in (* 2 (.readUnsignedShort in))) ;; interfaces
      (dotimes [_ (.readUnsignedShort in)] ;; fields
        (.skipBytes in 6)
        (skip-attributes in))
      (let [method-count (.readUnsignedShort in)]
        (loop [m 0 acc []]
          (if (= m method-count)
            acc
            (do (.skipBytes in 2) ;; access
                (let [name (utf8 (.readUnsignedShort in))
                      _ (.skipBytes in 2) ;; descriptor
                      attr-count (.readUnsignedShort in)
                      acc (loop [a 0 acc acc]
                            (if (= a attr-count)
                              acc
                              (let [attr-name (utf8 (.readUnsignedShort in))
                                    attr-len (.readInt in)]
                                (if (= "Code" attr-name)
                                  (do (.skipBytes in 4) ;; max stack + locals
                                      (let [code-len (.readInt in)]
                                        (.skipBytes in (- attr-len 8))
                                        (recur (inc a) (conj acc [name code-len]))))
                                  (do (.skipBytes in attr-len)
                                      (recur (inc a) acc))))))]
                  (recur (inc m) acc)))))))))

(deftest method-size-test
  (fs/with-temp-dir [tmp {}]
    (let [java (str (fs/path (System/getProperty "java.home") "bin"
                             (if (str/starts-with? (System/getProperty "os.name") "Windows")
                               "java.exe" "java")))
          ;; compile in a subprocess so this JVM's loaded namespaces stay untouched
          {:keys [exit err]} (sh java "-cp" (System/getProperty "java.class.path")
                                 "clojure.main" "-e"
                                 (str "(binding [*compile-path* " (pr-str (str tmp)) "]"
                                      "  (compile 'clj-kondo.impl.analyzer)"
                                      "  (compile 'clj-kondo.impl.linters))"))
          _ (is (zero? exit) err)
          offenders (for [f (fs/glob tmp "clj_kondo/**.class")
                          ;; __init classes run once at namespace load; their
                          ;; size costs startup only, not lint throughput
                          :when (not (str/ends-with? (fs/file-name f) "__init.class"))
                          [method size] (method-code-sizes f)
                          :when (> size huge-method-limit)]
                      (str (fs/file-name f) "/" method ": " size " bytes"))]
      (is (empty? offenders)
          (str "Methods over HotSpot's " huge-method-limit "-bytecode limit run "
               "interpreted, never JIT-compiled. Split them up:\n"
               (str/join "\n" offenders))))))
