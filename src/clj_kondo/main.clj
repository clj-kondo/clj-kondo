(ns clj-kondo.main
  (:gen-class)
  (:require [clj-kondo.core :refer [process-file]]))

;;;; printing

(defn print-findings [findings]
  (doseq [{:keys [:file :type :message :level :row :col]} findings]
    (println (str file ":" row ":" col ": " (name level) ": " message))))

(defn print-help []
  (println "Usage: --lint <file>. Use - for reading from stdin.")
  nil)


;;;; main

(defn -main [option & files]
  (when-let [files
             (case option
               "--lint" files
               "--help" (print-help)
               (print-help))]
    (let [findings (mapcat process-file files)]
      (print-findings findings))))

;;;; scratch

(comment
  ;; TODO: turn some of these into tests
  (spit "/tmp/id.clj" "(defn foo []\n  (def x 1))")
  (-main "/tmp/id.clj")
  (-main)
  (with-in-str "(defn foo []\n  (def x 1))" (-main "--lint" "-"))
  (with-in-str "(defn foo []\n  `(def x 1))" (-main "--lint" "-"))
  (with-in-str "(defn foo []\n  '(def x 1))" (-main "--lint" "-"))
  (inline-defs (p/parse-string-all "(defn foo []\n  (def x 1))"))
  (defn foo []\n  (def x 1))
  (nested-lets (p/parse-string-all "(let [i 10])"))
  (with-in-str "(let [i 10] (let [j 11]))" (-main "--lint" "-"))
  (with-in-str "(let [i 10] 1 (let [j 11]))" (-main "--lint" "-"))
  (with-in-str "(let [i 10] #_1 (let [j 11]))" (-main "--lint" "-"))
  (obsolete-do (p/parse-string-all "(do 1 (do 1 2))"))
  (with-in-str "(do 1)" (-main "--lint" "-"))
  (process-input "(fn [] (do 1 2))")
  (process-input "(let [] 1 2 (do 1 2 3))")
  (process-input "(defn foo [] (do 1 2 3))")
  (process-input "(defn foo [] (fn [] 1 2 3))")
  )
