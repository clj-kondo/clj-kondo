(ns clj-kondo.impl.analysis.java
  (:require [clojure.string :as str]))

(defn ->class-name [entry]
  (-> (str/replace entry "/" ".")
      (str/replace ".class" "")
      (str/replace ".java" "")))

(defn ->uri [jar entry file]
  (cond file (str "file:" file)
        (and jar entry)
        (str "jar:file:" jar "!/" entry)))

(defn java-class-def-analysis? [ctx]
  (-> ctx :config ))

(defn reg-java-class-def! [ctx {:keys [jar entry file]}]
  (when-let [class-name (cond entry
                              (->class-name entry)
                              file
                              (->class-name file))]
    (swap! (:analysis ctx)
           update :java-class-definitions conj
           {:class class-name
            :uri (->uri jar entry file)}))
  nil)

(defn reg-java-class-usage! [ctx class-name loc]
  (swap! (:analysis ctx)
         update :java-class-usages conj
         (merge {:class class-name
                 ;; TODO, fix for jar file
                 :uri (str "file:" (:filename ctx))
                 }
                loc))
  nil)
