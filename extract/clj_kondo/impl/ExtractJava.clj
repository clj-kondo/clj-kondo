(ns clj-kondo.impl.ExtractJava
  {:no-doc true}
  (:gen-class
   :methods [#^{:static true} [start [com.sun.javadoc.RootDoc] boolean]])
  (:require
   [clojure.java.io :as io]
   [cognitect.transit :as transit])
  (:import [javax.tools ToolProvider DocumentationTool]
           [clj_kondo.impl ExtractJava]))

(set! *warn-on-reflection* true)

(def extracted (atom nil))

(defn -start [^com.sun.javadoc.RootDoc root]
  (reset! extracted
          (vec (for [^com.sun.javadoc.ClassDoc c (.classes root)
                     ^com.sun.javadoc.MethodDoc m (.methods c)
                     :when (.isStatic m)]
                 {:class (.qualifiedName c)
                  :method (.name m)
                  :arity (count (.parameters m))})))
  true)

(def sconj (fnil conj #{}))

(defn -main [out & extra-args]
  (println "Extracting Java...")
  (let [dt (ToolProvider/getSystemDocumentationTool)
        fm (.getStandardFileManager dt nil nil nil)
        task (.getTask dt nil fm nil ExtractJava extra-args  nil)]
    (.call task))
  (println "done...")
  (let [extracted-java
        (reduce (fn [acc entry]
                  (let [ns (symbol (:class entry))
                        name (symbol (:method entry))]
                    (update acc ns
                            #(-> %
                                 (assoc-in [name :ns] ns)
                                 (assoc-in [name :name] name)
                                 (update-in [name :fixed-arities] sconj (:arity entry))))))
                {}
                @extracted)]
    (println "Writing cache files to" out)
    (doseq [[ns v] extracted-java]
      (let [file (io/file (str out "/" ns ".transit.json"))]
        (io/make-parents file)
        (let [bos (java.io.ByteArrayOutputStream. 1024)
              writer (transit/writer (io/output-stream bos) :json)]
          (transit/write writer v)
          (io/copy (.toByteArray bos) file))))))

;;;; Scratch

(comment
  )
