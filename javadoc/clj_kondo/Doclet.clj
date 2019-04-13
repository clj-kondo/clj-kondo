(ns clj-kondo.Doclet
  (:gen-class
   :methods [#^{:static true} [start [com.sun.javadoc.RootDoc] boolean]])
  (:import [javax.tools ToolProvider DocumentationTool])
  (:require
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [cognitect.transit :as transit]))

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
  (spit "/tmp/java.lang.edn"
        (vec (for [^com.sun.javadoc.ClassDoc c (.classes root)
                   ;;:let [_ (println (.name c))]
                   :when (contains? #{"Thread" "System" "Integer" "String"} (.name c))
                   ^com.sun.javadoc.MethodDoc m (.methods c)
                   :when (.isStatic m)]
               {:class (.qualifiedName c)
                :method (.name m)
                :arity (count (.parameters m))}
               #_(try (println (.name m))
                      ;; varargs seems to be always true
                      (println "varargs:" (.isVarArgs m))
                      (println "arity:" (count (.parameters m)))
                      (catch Throwable e
                        (println "something went wrong with" m))))))
  true)

;; requires JDK 11 now: JAVA_HOME=~/Downloads/jdk-11.0.2.jdk/Contents/Home
(def sconj (fnil conj #{}))

(defn -main []
  (println (System/getProperty "java.home"))
  (let [dt (ToolProvider/getSystemDocumentationTool)]
    (.run dt nil nil nil
          (into-array [;; "--help"
                       ;; "--add-modules" "java.base"
                       "-doclet" "clj_kondo.Doclet"
                       "-public"
                       "--source-path" "/Users/Borkdude/git/jdk/src/java.base/share/classes"
                       "java.lang"
                       ;;"--source-path" "/tmp/"
                       ;;"my.pack"
                       ]))
    (let [java-lang (let [edn (edn/read-string (slurp "/tmp/java.lang.edn"))]
                      (reduce (fn [acc entry]
                                (let [ns (:class entry)
                                      def (:method entry)
                                      qdef (symbol ns def)]
                                  (update acc (symbol ns)
                                          #(-> %
                                               (assoc-in [qdef :qname] qdef)
                                               (update-in [qdef :fixed-arities] sconj (:arity entry))))))
                              {}
                              edn))]
      (println java-lang)
      (doseq [[ns v] java-lang]
        (let [file (io/file (str "resources/clj_kondo/impl/cache/built_in/clj/" ns ".transit.json"))]
          (io/make-parents file)
          (let [bos (java.io.ByteArrayOutputStream. 1024)
                writer (transit/writer (io/output-stream bos) :json)]
            (transit/write writer v)
            (io/copy (.toByteArray bos) file)))
        #_(spit (str "resources/clj_kondo/impl/cache/built_in/clj/" ns ".transit.json") v)))))

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
