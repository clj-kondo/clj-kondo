(ns clj-kondo.impl.analysis.java
  (:require [clj-kondo.impl.utils :refer [->uri]]
            [clojure.string :as str]))

(defn ->class-name [entry]
  (-> (str/replace entry "/" ".")
      (str/replace ".class" "")
      (str/replace ".java" "")))

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
            :uri (->uri jar entry file)
            :filename (or file
                          (str jar ":" entry))})))

(defn reg-java-class-usage! [ctx class-name loc]
  (swap! (:analysis ctx)
         update :java-class-usages conj
         (merge {:class class-name
                 :uri (:uri ctx)
                 :filename (:filename ctx)}
                loc))
  nil)
