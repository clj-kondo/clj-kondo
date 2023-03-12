(ns clj-kondo.impl.analysis.java
  (:require
   [clj-kondo.impl.utils :refer [->uri]]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   [java.io File InputStream]
   [java.util.jar JarFile JarFile$JarFileEntry]
   (org.objectweb.asm
    ClassReader
    ClassVisitor
    Opcodes
    Type)))

(set! *warn-on-reflection* true)

(defn ^:private input-stream->bytes ^bytes [^InputStream input-stream]
  (with-open [xin input-stream
              xout (java.io.ByteArrayOutputStream.)]
    (io/copy xin xout)
    (.toByteArray xout)))

(defn entry->class-name [entry]
  (-> entry
      (str/replace "/" ".")
      (str/replace "\\" ".")
      (str/replace ".class" "")
      (str/replace ".java" "")))

(def ^:private opcode->flags
  {Opcodes/ACC_PUBLIC #{:public}
   Opcodes/ALOAD #{:public :field :static}
   Opcodes/SIPUSH #{:public :field :final}
   Opcodes/LCONST_0 #{:public :method :static}})

(defn ^:private class-is->class-info
  "Parse class-bytes using ASM."
  [^InputStream class-is]
  (let [class-reader (ClassReader. (input-stream->bytes class-is))
        class-name (str/replace (.getClassName class-reader) "/" ".")
        result* (atom {class-name {:members []}})]
    (.accept
     class-reader
     (proxy [ClassVisitor] [Opcodes/ASM9]
       (visitField [access ^String name ^String desc signature value]
         (let [flags (opcode->flags access)]
           (when (:public flags)
             (swap! result* update-in [class-name :members] conj
                    {:name name
                     :flags (conj flags :field)
                     :type (.getClassName (Type/getType desc))})))
         nil)
       (visitMethod [access ^String name ^String desc signature exceptions]
         (let [flags (opcode->flags access)]
           (when (:public flags)
             (swap! result* update-in [class-name :members] conj
                    {:name name
                     :parameter-types (mapv #(.getClassName ^Type %) (Type/getArgumentTypes desc))
                     :flags (conj flags :method)
                     :return-type (.getClassName (Type/getReturnType desc))})))
         nil))
     ClassReader/SKIP_DEBUG)
    @result*))

(defn source->class-name [file]
  (let [fname (entry->class-name file)
        fname (str/replace fname "\\" ".")
        class-name (last (str/split fname #"\."))]
    (with-open [file-reader (io/reader file)]
      (binding [*in* file-reader]
        (loop []
          (if-let [next-line (read-line)]
            (if-let [[_ package] (re-matches #"\s*package\s+(\S*)\s*;\s*" next-line)]
              (str package "." class-name)
              (recur))
            class-name))))))

(defn analyze-class-defs? [ctx]
  (:analyze-java-class-defs? ctx))

(defn reg-class-def! [ctx {:keys [^JarFile jar ^JarFile$JarFileEntry entry filename ^File file]}]
  (when (analyze-class-defs? ctx)
    (let [class-is (or (and jar entry (.getInputStream jar entry))
                       (io/input-stream filename))
          uri (if jar
                (->uri (str (.getCanonicalPath file)) (.getName entry) nil)
                (->uri nil nil filename))]
      (if (and (str/ends-with? filename ".class")
               (:analyze-java-member-defs? ctx))
        (doseq [[class-name class-info] (class-is->class-info class-is)]
          (swap! (:analysis ctx)
                 update :java-class-definitions conj
                 {:class class-name
                  :uri uri
                  :filename filename})
          (doseq [member (:members class-info)]
            (swap! (:analysis ctx)
                   update :java-member-definitions conj
                   (merge {:class class-name
                           :uri uri}
                          member))))
        (when-let [class-name (if entry
                                (entry->class-name entry)
                                (when (str/ends-with? filename ".java")
                                  (source->class-name filename)))]
          (swap! (:analysis ctx)
                 update :java-class-definitions conj
                 {:class class-name
                  :uri uri
                  :filename filename}))))))

(defn analyze-class-usages? [ctx]
  (and (:analyze-java-class-usages? ctx)
       (identical? :clj (:lang ctx))))

(defn reg-class-usage!
  ([ctx class-name loc+data]
   (reg-class-usage! ctx class-name loc+data nil))
  ([ctx class-name loc+data name-meta]
   (when (analyze-class-usages? ctx)
     (let [constructor-expr (:constructor-expr ctx)
           loc+data* loc+data
           loc+data (merge loc+data (meta constructor-expr))
           name-meta (or name-meta
                         (when constructor-expr
                           loc+data*))]
       (swap! (:analysis ctx)
              update :java-class-usages conj
              (merge {:class class-name
                      :uri (:uri ctx)
                      :filename (:filename ctx)}
                     loc+data
                     (when name-meta
                       {:name-row (:row name-meta)
                        :name-col (:col name-meta)
                        :name-end-row (:end-row name-meta)
                        :name-end-col (:end-col name-meta)})))))
   nil))
