(ns clj-kondo.impl.analysis.java
  {:no-doc true}
  (:require
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.utils :as utils]
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
    EnumConstantDeclaration
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

#_(defn ^:private opcode->flags []
    {Opcodes/ACC_PUBLIC #{:public}
     Opcodes/ALOAD #{:public :field :static}
     Opcodes/SIPUSH #{:public :field :final}
     Opcodes/LCONST_0 #{:public :method :static}})

(defn- opcode->flags
  "Thanks @hiredman for https://downey.family/p/2024-02-22/modifiers.clj.html. Generated with:"
  #_(clojure.pprint/pprint `(fn [~'x] (cond-> #{} ~@(->> (.getFields clojure.asm.Opcodes) (filter #(.startsWith (.getName %) "ACC_")) (map (fn [field] `[(= (bit-and ~'x ~(symbol "clojure.asm.Opcodes" (.getName field))) ~(symbol "clojure.asm.Opcodes" (.getName field))) (conj ~(keyword (-> (.getName field) (.replaceAll "^ACC_" "") (.toLowerCase))))])) (apply concat)))))

  [x]
  (clojure.core/cond->
   #{}
    (clojure.core/=
     (clojure.core/bit-and x Opcodes/ACC_PUBLIC)
     Opcodes/ACC_PUBLIC)
    (clojure.core/conj :public)
    #_#_(clojure.core/=
         (clojure.core/bit-and x Opcodes/ACC_PRIVATE)
         Opcodes/ACC_PRIVATE)
      (clojure.core/conj :private)
    #_#_(clojure.core/=
         (clojure.core/bit-and x Opcodes/ACC_PROTECTED)
         Opcodes/ACC_PROTECTED)
      (clojure.core/conj :protected)
    (clojure.core/=
     (clojure.core/bit-and x Opcodes/ACC_STATIC)
     Opcodes/ACC_STATIC)
    (clojure.core/conj :static)
    (clojure.core/=
     (clojure.core/bit-and x Opcodes/ACC_FINAL)
     Opcodes/ACC_FINAL)
    (clojure.core/conj :final)
    #_(clojure.core/=
       (clojure.core/bit-and x Opcodes/ACC_SUPER)
       Opcodes/ACC_SUPER)
    #_(clojure.core/conj :super)
    #_(clojure.core/=
       (clojure.core/bit-and x Opcodes/ACC_SYNCHRONIZED)
       Opcodes/ACC_SYNCHRONIZED)
    #_(clojure.core/conj :synchronized)
    #_(clojure.core/=
       (clojure.core/bit-and x Opcodes/ACC_OPEN)
       Opcodes/ACC_OPEN)
    #_(clojure.core/conj :open)
    #_(clojure.core/=
       (clojure.core/bit-and x Opcodes/ACC_TRANSITIVE)
       Opcodes/ACC_TRANSITIVE)
    #_(clojure.core/conj :transitive)
    #_(clojure.core/=
       (clojure.core/bit-and x Opcodes/ACC_VOLATILE)
       Opcodes/ACC_VOLATILE)
    #_(clojure.core/conj :volatile)
    #_#_(clojure.core/=
         (clojure.core/bit-and x Opcodes/ACC_BRIDGE)
         Opcodes/ACC_BRIDGE)
      (clojure.core/conj :bridge)
    #_#_(clojure.core/=
         (clojure.core/bit-and x Opcodes/ACC_STATIC_PHASE)
         Opcodes/ACC_STATIC_PHASE)
      (clojure.core/conj :static_phase)
    #_#_(clojure.core/=
         (clojure.core/bit-and x Opcodes/ACC_VARARGS)
         Opcodes/ACC_VARARGS)
      (clojure.core/conj :varargs)
    #_#_(clojure.core/=
         (clojure.core/bit-and x Opcodes/ACC_TRANSIENT)
         Opcodes/ACC_TRANSIENT)
      (clojure.core/conj :transient)
    #_#_(clojure.core/=
         (clojure.core/bit-and x Opcodes/ACC_NATIVE)
         Opcodes/ACC_NATIVE)
      (clojure.core/conj :native)
    (clojure.core/=
     (clojure.core/bit-and x Opcodes/ACC_INTERFACE)
     Opcodes/ACC_INTERFACE)
    (clojure.core/conj :interface)
    #_#_(clojure.core/=
         (clojure.core/bit-and x Opcodes/ACC_ABSTRACT)
         Opcodes/ACC_ABSTRACT)
      (clojure.core/conj :abstract)
    #_#_(clojure.core/=
         (clojure.core/bit-and x Opcodes/ACC_STRICT)
         Opcodes/ACC_STRICT)
      (clojure.core/conj :strict)
    #_#_(clojure.core/=
         (clojure.core/bit-and x Opcodes/ACC_SYNTHETIC)
         Opcodes/ACC_SYNTHETIC)
      (clojure.core/conj :synthetic)
    #_#_(clojure.core/=
         (clojure.core/bit-and x Opcodes/ACC_ANNOTATION)
         Opcodes/ACC_ANNOTATION)
      (clojure.core/conj :annotation)
    #_#_(clojure.core/=
         (clojure.core/bit-and x Opcodes/ACC_ENUM)
         Opcodes/ACC_ENUM)
      (clojure.core/conj :enum)
    #_#_(clojure.core/=
         (clojure.core/bit-and x Opcodes/ACC_MANDATED)
         Opcodes/ACC_MANDATED)
      (clojure.core/conj :mandated)
    #_#_(clojure.core/=
         (clojure.core/bit-and x Opcodes/ACC_MODULE)
         Opcodes/ACC_MODULE)
      (clojure.core/conj :module)
    #_#_(clojure.core/=
         (clojure.core/bit-and x Opcodes/ACC_DEPRECATED)
         Opcodes/ACC_DEPRECATED)
      (clojure.core/conj :deprecated)))

(defn ^:private modifier-keyword->flag []
  (reduce #(assoc %1 %2 (keyword (str/lower-case (.asString ^Modifier$Keyword %2))))
          {}
          (Modifier$Keyword/values)))

(defn ^:private class-is->class-info
  "Parse class-bytes using ASM."
  [^InputStream class-is]
  (let [class-reader (ClassReader. (input-stream->bytes class-is))
        class-name (str/replace (.getClassName class-reader) "/" ".")
        result* (atom {class-name {:members [] :flags nil}})]
    (.accept
     class-reader
     (proxy [ClassVisitor] [Opcodes/ASM9]
       (visit [version access name signature superName interfaces]
         (swap! result* assoc-in [class-name :flags] (opcode->flags access))
         nil)
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
    ConstructorDeclaration :method
    EnumConstantDeclaration :field))

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
         (fn [classes ^com.github.javaparser.ast.body.TypeDeclaration class-or-interface]
           (if-let [class-name (.orElse (.getFullyQualifiedName class-or-interface) nil)]
             (let [members (->> (concat
                                 (.findAll class-or-interface FieldDeclaration)
                                 (.findAll class-or-interface ConstructorDeclaration)
                                 (.findAll class-or-interface MethodDeclaration)
                                 (.findAll class-or-interface EnumConstantDeclaration))
                                (keep #(node->member % modifier-keyword->flag)))
                   flags (set (map #(modifier-keyword->flag
                                     (.getKeyword ^Modifier %))
                                   (.getModifiers class-or-interface)))
                   flags (if (and (instance? ClassOrInterfaceDeclaration class-or-interface)
                                  (.isInterface ^ClassOrInterfaceDeclaration class-or-interface))
                           (conj flags :interface)
                           flags)]
               (assoc classes class-name {:members (vec members)
                                          :flags flags}))
             classes))
         {}
         (.findAll compilation com.github.javaparser.ast.body.TypeDeclaration)))
      (catch Throwable e
        (binding [*out* *err*]
          (println "Error parsing java file" filename "with error" e))))))

(defn analyze-class-defs? [ctx]
  (:analyze-java-class-defs? ctx))

(defn reg-class-def! [ctx {:keys [^JarFile jar ^JarFile$JarFileEntry entry filename ^File file]}]
  (when (analyze-class-defs? ctx)
    (let [uri (if jar
                (utils/->uri (str (.getCanonicalPath file)) (.getName entry) nil)
                (utils/->uri nil nil filename))
          class-is (or (and jar entry (.getInputStream jar entry))
                       (io/input-stream filename))
          class-by-info (with-open [is ^InputStream class-is]
                          (if (str/ends-with? filename ".class")
                            (class-is->class-info is)
                            (source-is->java-member-definitions is filename)))]
      (doseq [[class-name class-info] class-by-info]
        (let [flags (:flags class-info)
              class-def (cond-> {:class class-name
                                 :uri uri
                                 :filename filename
                                 :flags flags}
                          (contains? flags :interface)
                          (assoc :interface? true))]
          (swap! (:analysis ctx)
                 update :java-class-definitions conj
                 class-def))
        (when (:analyze-java-member-defs? ctx)
          (doseq [member (:members class-info)]
            (swap! (:analysis ctx)
                   update :java-member-definitions conj
                   (merge {:class class-name
                           :uri uri}
                          member))))))))

(defn analyze-class-usages? [ctx]
  (identical? :clj (:lang ctx)))

(defn lint-discouraged-method! [ctx class-name method-name loc+data]
  (let [discouraged-meth-config
        (get-in (:config ctx) [:linters :discouraged-java-method])]
    (when-not (or (identical? :off (:level discouraged-meth-config))
                  (empty? (dissoc discouraged-meth-config :level)))
      (when-let [cfg (get-in discouraged-meth-config [(symbol class-name) (symbol method-name)])]
        (findings/reg-finding! ctx
                               (assoc loc+data
                                      :filename (:filename ctx)
                                      :level (or (:level cfg) (:level discouraged-meth-config))
                                      :type :discouraged-java-method
                                      :message (or (:message cfg)
                                                   (str "Discouraged method: " method-name))))))))

(defn reg-class-usage!
  ([ctx class-name method-name loc+data]
   (reg-class-usage! ctx class-name method-name loc+data nil nil))
  ([ctx class-name method-name loc+data name-meta opts]
   (let [constructor-expr (:constructor-expr ctx)
         loc+data* loc+data
         loc+data (merge loc+data (meta constructor-expr))
         name-meta (or name-meta
                       (when constructor-expr
                         loc+data*))]
     (when method-name
       (lint-discouraged-method! ctx class-name method-name
                                 loc+data))
     (swap! (:java-class-usages ctx)
            conj
            (merge {:class class-name
                    :uri (:uri ctx)
                    :filename (:filename ctx)
                    :call (:call opts)
                    :config (:config ctx)
                    :lang (:lang ctx)}
                   loc+data
                   (when method-name
                     {:method-name method-name})
                   (when name-meta
                     {:name-row (:row name-meta)
                      :name-col (:col name-meta)
                      :name-end-row (:end-row name-meta)
                      :name-end-col (:end-col name-meta)}))))
   nil))

#_:clj-kondo/ignore
(comment
  (def x (source-is->java-member-definitions (io/input-stream "/Users/borkdude/.cache/clojure-lsp/jdk/java.base/java/lang/System.java") "/Users/borkdude/.cache/clojure-lsp/jdk/java.base/java/lang/System.java"))
  x
  (keys x)
  (def sys (get x "java.lang.System"))
  (defn ana->cached [name ana]
    (let [members (:members ana)
          grouped (group-by :name members)]
      (utils/update-vals grouped (fn [dudes]
                                   (-> dudes first
                                       (select-keys [:flags]))))))
  (ana->cached "java.lang.System" sys)
  (def clazz (io/resource "java/time/temporal/ChronoField.class"))
  (class-is->class-info (io/input-stream clazz)))
