(ns clj-kondo.impl.analyzer.java
  (:require [clojure.string :as str]))

(defn entry->class-name [entry]
  (-> (str/replace entry "/" ".")
      (str/replace ".class" "")
      (str/replace ".java" "")))

(defn reg-java-class! [ctx {:keys [entry file] :as opts}]
  (when file (prn :file file))
  (when-let [class-name (cond entry
                              (entry->class-name entry)
                              file
                              (entry->class-name file))]
    (swap! (:java-analysis ctx)
           update-in [:classes class-name] merge opts))
  nil)
