(ns clj-kondo.Doclet
  (:gen-class
   :methods [#^{:static true} [start [com.sun.javadoc.RootDoc] boolean]])
  (:import [javax.tools ToolProvider DocumentationTool])
  (:require
   [clojure.java.io :as io]))

(set! *warn-on-reflection* true)

#_(defn write-class [docmap]
  (def d docmap))

(declare extract-docs)
#_(def doc-keys 
  {
   "name" #(.name %)
   "annotations" #(map extract-docs (.annotations %))
   "qualifiedName" #(.qualifiedName %)
   "methods" #(map extract-docs (.methods %))
   "docString" #(.commentText %)
   "fields" #(map extract-docs (.fields %))
   "interfaces" #(map extract-docs (.interfaceTypes %))
   "innerClasses" #(map extract-docs (.innerClasses %))
   "constructors" #(map extract-docs (.constructors %))
   "parameters" #(map extract-docs (.parameters %))
   "typeName" #(.typeName (.type %))
   "qualifiedTypeName" #(.qualifiedTypeName (.type %))
   "modifiers" #(.modifiers %)
   "returnType" #(.qualifiedTypeName (.returnType %))
   "annotationType" #(.qualifiedTypeName (.annotationType %))
   "elementValues" #(map extract-docs (.elementValues %))
   "element" #(extract-docs (.element %))
  })

#_(defn extract-docs [doc-obj]
  (println "class" (type doc-obj))
  (def do doc-obj)
  (reduce-kv
    (fn [docmap k extractor]
      (try
        (assoc docmap k (extractor doc-obj))
        (catch Exception e docmap)))
   {} doc-keys))

(defn start [root]
  (def r root)
  #_(doall (pmap write-class (map extract-docs (.classes root))))
  true)

(defn -start [^com.sun.javadoc.RootDoc root]
  (doseq [^com.sun.javadoc.ClassDoc c (.classes root)
          ^com.sun.javadoc.MethodDoc m (.methods c)]
    (try (println (.name m))
         (println "varargs:" (.isVarArgs m))
         (println "arity:" (count (.parameters m)))
         (println "return type:" (.returnType m))
         (catch Throwable e
           (println "something went wrong with" m))))
  true)

;; requires JDK 11 now: JAVA_HOME=~/Downloads/jdk-11.0.2.jdk/Contents/Home
(defn -main []
  (println (System/getProperty "java.home"))
  (let [dt (ToolProvider/getSystemDocumentationTool)]
    (.run dt nil nil nil
          (into-array [;; "--help"
                       ;; "--add-modules" "java.base"
                       "-doclet" "clj_kondo.Doclet"
                       "-public"
                       ;; "--source-path" "/Users/Borkdude/git/jdk/src/java.base/share/classes"
                       "--source-path" "/tmp/"
                       "my.pack"
                       ]))))

(comment
  (count (.classes r))
  (map (fn [i c] [i (.name c)]) (range) (.classes r))
  (def t (aget (.classes r) 15))
  (count (.methods t))
  (map #(.name %) (.methods t))
  (def sleep (aget (.methods t) 2))
  (.isVarArgs sleep)
  (count (.parameters sleep))
  (def sleep2 (aget (.methods t) 3))
  (count (.parameters sleep2))
  )
