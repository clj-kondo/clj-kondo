(ns clj-kondo.impl.analyzer.javadoc
  (:refer-clojure :exclude [resolve])
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.io StringReader StringWriter)
   (javax.lang.model.element Element ElementKind ExecutableElement
                             TypeElement VariableElement)
   (javax.swing.text.html HTML$Tag HTMLEditorKit$ParserCallback)
   (javax.swing.text.html.parser ParserDelegator)
   (javax.tools ToolProvider)
   (jdk.javadoc.doclet Doclet DocletEnvironment)))

(def result (atom nil))

(defn parse-java
  "Load and parse the resource path, returning a `DocletEnvironment` object."
  [path module]
  (let [path (io/file path)
        tmpdir   (System/getProperty "java.io.tmpdir")
        tmpfile  (io/file tmpdir (.getName (io/file path)))
        compiler (ToolProvider/getSystemDocumentationTool)
        sources  (-> (.getStandardFileManager compiler nil nil nil)
                     (.getJavaFileObjectsFromFiles [tmpfile]))
        doclet (reify Doclet
                 (init [this _ _]
                   (prn :init)
                   (reset! result nil))
                 (run [this root]
                   (reset! result root) true)
                 (getSupportedOptions [this] #{}))
        doclet-class (class doclet)
        out      (StringWriter.) ; discard compiler messages
        opts     (apply conj ["--show-members" "private"
                              "--show-types" "private"
                              "--show-packages" "all"
                              "--show-module-contents" "all"
                              #_"-quiet"]
                        (when module
                          ["--patch-module" (str module "=" tmpdir)]))]
    (spit tmpfile (slurp path))
    (.call (.getTask compiler out nil nil doclet-class opts sources))
    (.delete tmpfile)
    @result))

(defn typesym
  "Using parse tree info, return the type's name equivalently to the `typesym`
  function in `orchard.java`."
  ([n ^DocletEnvironment env]
   (let [t (str/replace (str n) #"<.*>" "") ; drop generics
         util (.getElementUtils env)]
     (if-let [c (.getTypeElement util t)]
       (let [pkg (str (.getPackageOf util c) ".")
             cls (-> (str/replace-first t pkg "")
                     (str/replace "." "$"))]
         (symbol (str pkg cls))) ; classes
       (symbol t)))))            ; primitives

(defn position
  "Get line and column of `Element` e using parsed source information in env"
  [e ^DocletEnvironment env]
  (let [trees (.getDocTrees env)]
    (when-let [path (.getPath trees e)]
      (let [file (.getCompilationUnit path)
            lines (.getLineMap file)
            pos (.getStartPosition (.getSourcePositions trees)
                                   file (.getLeaf path))]
        {:line (.getLineNumber lines pos)
         :column (.getColumnNumber lines pos)}))))

(defprotocol Parsed
  (parse-info* [o env]))

(defn parse-info
  [o env]
  (merge (parse-info* o env)
         #_(docstring o env)
         (position o env)))

(extend-protocol Parsed
  ExecutableElement ; => method, constructor
  (parse-info* [m env]
    {:name (if (= (.getKind m) ElementKind/CONSTRUCTOR)
             (-> m .getEnclosingElement (typesym env)) ; class name
             (-> m .getSimpleName str symbol))         ; method name
     :type (-> m .getReturnType (typesym env))
     :argtypes (mapv #(-> ^VariableElement % .asType (typesym env)) (.getParameters m))
     :argnames (mapv #(-> ^VariableElement % .getSimpleName str symbol) (.getParameters m))})

  VariableElement ; => field, enum constant
  (parse-info* [f env]
    {:name (-> f .getSimpleName str symbol)
     :type (-> f .asType (typesym env))})

  TypeElement ; => class, interface, enum
  (parse-info* [c env]
    {:class   (typesym c env)
     :members (->> (.getEnclosedElements c)
                   (filter #(#{ElementKind/CONSTRUCTOR
                               ElementKind/METHOD
                               ElementKind/FIELD
                               ElementKind/ENUM_CONSTANT}
                             (.getKind ^Element %)))
                   (map #(parse-info % env))
                   ;; Index by name, argtypes. Args for fields are nil.
                   (group-by :name)
                   (reduce (fn [ret [n ms]]
                             (assoc ret n (zipmap (map :argtypes ms) ms)))
                           {}))}))

#_(defn module-name
  "Return the module name, or nil if modular"
  [klass]
  (some-> klass ^Class .getModule .getName))

#_(defn source-path
  "Return the relative `.java` source path for the top-level class."
  [klass]
  (when-let [^Class cls klass]
    (let [path (-> (.getName cls)
                   (str/replace #"\$.*" "")
                   (str/replace "." "/")
                   (str ".java"))]
      (if-let [module (-> cls .getModule .getName)]
        (str module "/" path)
        path))))

(defn source-info
  "If the source for the Java class is available on the classpath, parse it
  and return info to supplement reflection. Specifically, this includes source
  file and position, docstring, and argument name info. Info returned has the
  same structure as that of `orchard.java/reflect-info`."
  [path]
  (when-let [^DocletEnvironment root (parse-java path nil #_(module-name klass))]
    (try
      (assoc (->> (.getIncludedElements root)
                  (filter #(#{ElementKind/CLASS
                              ElementKind/INTERFACE
                              ElementKind/ENUM}
                            (.getKind ^Element %)))
                  (map #(parse-info % root))
                  (first))
             :file path
             :path (.getPath (io/file path)))
      (finally (.close (.getJavaFileManager root))))))

(defn -main [& [java-file]]
  (prn (source-info java-file)))
