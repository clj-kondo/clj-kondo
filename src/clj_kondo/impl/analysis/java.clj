(ns clj-kondo.impl.analysis.java
  {:no-doc true}
  (:require
   [clj-kondo.impl.utils :refer [->uri]]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str])
  (:import
   [com.github.javaparser JavaParser Range]
   [com.github.javaparser.ast
    CompilationUnit
    Modifier
    Modifier$Keyword
    Node]
   [com.github.javaparser.ast.body
    ClassOrInterfaceDeclaration
    ConstructorDeclaration
    FieldDeclaration
    MethodDeclaration
    Parameter
    VariableDeclarator]
   [com.github.javaparser.ast.comments Comment]
   [com.github.javaparser.ast.expr SimpleName]
   [com.github.javaparser.metamodel PropertyMetaModel]
   [java.io File InputStream]
   [java.util.jar JarFile JarFile$JarFileEntry]
   [org.objectweb.asm
    ClassReader
    ClassVisitor
    Opcodes
    Type]))

(set! *warn-on-reflection* true)

(defn ^:private input-stream->bytes ^bytes [^InputStream input-stream]
  (with-open [xin input-stream
              xout (java.io.ByteArrayOutputStream.)]
    (io/copy xin xout)
    (.toByteArray xout)))

(defn ^:private opcode->flags []
  {Opcodes/ACC_PUBLIC #{:public}
   Opcodes/ALOAD #{:public :field :static}
   Opcodes/SIPUSH #{:public :field :final}
   Opcodes/LCONST_0 #{:public :method :static}})

(defn ^:private modifier-keyword->flag []
  (reduce #(assoc %1 %2 (keyword (str/lower-case (.asString ^Modifier$Keyword %2))))
          {}
          (Modifier$Keyword/values)))

(defn ^:private class-is->class-info
  "Parse class-bytes using ASM."
  [^InputStream class-is]
  (let [class-reader (ClassReader. (input-stream->bytes class-is))
        class-name (str/replace (.getClassName class-reader) "/" ".")
        result* (atom {class-name {:members []}})
        opcode->flags (opcode->flags)]
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

(defn ^:private node->flag-member-type [node]
  (condp = (type node)
    FieldDeclaration :field
    MethodDeclaration :method
    ConstructorDeclaration :method))

(defn ^:private node->location [^Node node]
  (when-let [^Range range (.orElse (.getRange node) nil)]
    {:row (.-line (.-begin range))
     :col (.-column (.-begin range))
     :end-row (.-line (.-end range))
     :end-col (.-column (.-end range))}))

(defn ^:private node->member
  [^Node node modifier-keyword->flag]
  (let [member (for [^PropertyMetaModel model (.getAllPropertyMetaModels (.getMetaModel node))
                     :let [value-or-list (.getValue model node)]]
                 (condp identical? (.getType model)
                   Modifier {:flags (set (map #(modifier-keyword->flag (.getKeyword ^Modifier %)) value-or-list))}
                   Comment (some->> ^Comment value-or-list .asString (hash-map :doc))
                   VariableDeclarator {:name (.asString (.getName ^VariableDeclarator (first value-or-list)))
                                       :type (.asString (.getType ^VariableDeclarator (first value-or-list)))}
                   Parameter {:parameters (mapv #(.toString ^Parameter %) value-or-list)}
                   SimpleName {:name (.asString ^SimpleName value-or-list)}
                   com.github.javaparser.ast.type.Type {:return-type (.asString ^com.github.javaparser.ast.type.Type value-or-list)}
                   nil))
        member (reduce merge {} member)]
    (when-not (contains? (:flags member) :private)
      (-> member
          (merge (some-> node node->location))
          (update :flags set/union #{(node->flag-member-type node)})))))

(defn ^:private source-is->java-member-definitions [^InputStream source-input-stream filename]
  (let [modifier-keyword->flag (modifier-keyword->flag)]
    (try
      (when-let [compilation ^CompilationUnit (.orElse (.getResult (.parse (JavaParser.) source-input-stream)) nil)]
        (reduce
         (fn [classes ^ClassOrInterfaceDeclaration class-or-interface]
           (let [class-name (.get (.getFullyQualifiedName class-or-interface))
                 members (->> (concat
                               (.findAll class-or-interface FieldDeclaration)
                               (.findAll class-or-interface ConstructorDeclaration)
                               (.findAll class-or-interface MethodDeclaration))
                              (keep #(node->member % modifier-keyword->flag)))]
             (assoc classes class-name {:members (vec members)})))
         {}
         (.findAll compilation ClassOrInterfaceDeclaration)))
      (catch Throwable e
        (binding [*out* *err*]
          (println "Error parsing java file" filename "with error" e))))))

(defn analyze-class-defs? [ctx]
  (:analyze-java-class-defs? ctx))

(defn reg-class-def! [ctx {:keys [^JarFile jar ^JarFile$JarFileEntry entry filename ^File file]}]
  (when (analyze-class-defs? ctx)
    (let [uri (if jar
                (->uri (str (.getCanonicalPath file)) (.getName entry) nil)
                (->uri nil nil filename))
          class-is (or (and jar entry (.getInputStream jar entry))
                       (io/input-stream filename))
          class-by-info (with-open [is ^InputStream class-is]
                          (if (str/ends-with? filename ".class")
                            (class-is->class-info is)
                            (source-is->java-member-definitions is filename)))]
      (doseq [[class-name class-info] class-by-info]
        (swap! (:analysis ctx)
               update :java-class-definitions conj
               {:class class-name
                :uri uri
                :filename filename})
        (when (:analyze-java-member-defs? ctx)
          (doseq [member (:members class-info)]
            (swap! (:analysis ctx)
                   update :java-member-definitions conj
                   (merge {:class class-name
                           :uri uri}
                          member))))))))

(defn analyze-class-usages? [ctx]
  (and (:analyze-java-class-usages? ctx)
       (identical? :clj (:lang ctx))))

(defn reg-class-usage!
  ([ctx class-name method-name loc+data]
   (reg-class-usage! ctx class-name method-name loc+data nil))
  ([ctx class-name method-name loc+data name-meta]
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
                     (when method-name
                       {:method-name method-name})
                     (when name-meta
                       {:name-row (:row name-meta)
                        :name-col (:col name-meta)
                        :name-end-row (:end-row name-meta)
                        :name-end-col (:end-col name-meta)})))))
   nil))
