(ns clj-kondo.impl.analyzer.java
  (:require [clojure.java.io :as io])
  (:import ;; [com.github.javaparser.ast.visitor VoidVisitorAdapter]
   [com.github.javaparser JavaParser]
   [com.github.javaparser.ast Node NodeList]
   [com.github.javaparser.ast CompilationUnit]
   [com.github.javaparser.ast.body ClassOrInterfaceDeclaration]
   [com.github.javaparser.metamodel BaseNodeMetaModel
    JavaParserMetaModel PropertyMetaModel]))

(set! *warn-on-reflection* true)

(require '[clojure.pprint :as pp])

;; see https://tomassetti.me/getting-started-with-javaparser-analyzing-java-code-programmatically/

(defn serialize [^String name ^Node node]
  {:name name
   :val
   (when node
     (when-let [^BaseNodeMetaModel nmm (.orElse (JavaParserMetaModel/getNodeMetaModel (class node)) nil)]
       (doall (for [^PropertyMetaModel model (.getAllPropertyMetaModels nmm)
                    :let [name (.getName model)
                          val (.getValue model node)
                          _ (prn (class val))
                          ]
                    :when (or
                           (instance? NodeList val)
                           (and (instance? ClassOrInterfaceDeclaration val)
                                (= "name" name)))

                    ]
                {:name name
                 :class (str (class val))
                 :children (if (.isNodeList model)
                             (mapv #(serialize name %) val)
                             (serialize name val))}))
       #_(let [visitor (proxy [VoidVisitorAdapter] []
                         (visit [decl arg]
                           (proxy-super visit decl arg)
                           (prn (class decl))
                           #_(when (instance? ClassOrInterfaceDeclaration decl)
                               (prn decl arg)
                               )))]
           (.visit visitor cu nil))))})

(defn -main [& [java-file]]
  (let [p (JavaParser.)
        parsed (.parse p (.toPath (io/file java-file)))
        ;; note: unsafe
        cu (.get (.getResult parsed))
        ]
    ;; (prn (pprint (bean cu)) (type cu))
    ;; (prn cu)
    ;; (prn (class cu))
    (let [^BaseNodeMetaModel nmm (.get (JavaParserMetaModel/getNodeMetaModel (class cu)))]
      (pp/pprint (serialize "" cu))
      #_(let [visitor (proxy [VoidVisitorAdapter] []
                        (visit [decl arg]
                          (proxy-super visit decl arg)
                          (prn (class decl))
                          #_(when (instance? ClassOrInterfaceDeclaration decl)
                              (prn decl arg)
                              )))]
          (.visit visitor cu nil)))))
