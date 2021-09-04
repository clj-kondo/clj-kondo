(ns clj-kondo.impl.analyzer.java
  (:require [clojure.java.io :as io])
  (:import [com.github.javaparser.ast Node]
           [com.github.javaparser.ast.visitor VoidVisitorAdapter]
           [com.github.javaparser.ast.body ClassOrInterfaceDeclaration]
           [com.github.javaparser JavaParser]))

(set! *warn-on-reflection* true)

(require '[clojure.pprint :refer [pprint]])

;; see https://tomassetti.me/getting-started-with-javaparser-analyzing-java-code-programmatically/

(defn -main [& [java-file]]
  (let [p (JavaParser.)
        parsed (.parse p (.toPath (io/file java-file)))
        ;; note: unsafe
        cu (.get (.getResult parsed))
        ]
    ;; (prn (pprint (bean cu)) (type cu))
    ;; (prn cu)
    (let [visitor (proxy [VoidVisitorAdapter] []
                    (visit [decl arg]
                      (proxy-super visit decl arg)
                      (prn (class decl))
                      #_(when (instance? ClassOrInterfaceDeclaration decl)
                        (prn decl arg)
                        )))]
      (.visit visitor cu nil))))
