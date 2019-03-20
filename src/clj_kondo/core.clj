(ns clj-kondo.core
  (:gen-class)
  (:require [rewrite-clj.parser :as p]
            [clj-kondo.utils :as utils
             :refer [uneval?
                     remove-whitespace
                     call?
                     node->line]]
            [clojure.string :as str]))

;;;; inline def

(defn inline-def* [rw-expr in-def?]
  (let [current-def? (call? rw-expr 'def 'defn)
        new-in-def? (and (not (contains? '#{:syntax-quote :quote}
                                         (:tag rw-expr)))
                         (or in-def? current-def?))]
    (if (and in-def? current-def?)
      [rw-expr]
      (when (:children rw-expr)
        (mapcat #(inline-def* % new-in-def?) (:children rw-expr))))))

(defn inline-def [parsed-expressions]
  (map #(node->line % :warning :inline-def "inline def")
       (inline-def* parsed-expressions false)))

;;;; obsolete let

(defn obsolete-let* [{:keys [:children] :as rw-expr}
                     parent-let?]
  (let [current-let? (call? rw-expr 'let)]
    (cond (and current-let? parent-let?)
          [rw-expr]
          current-let?
          (let [;; skip let keywords and bindings
                children (nnext children)]
            (concat (obsolete-let* (first children) current-let?)
                    (mapcat #(obsolete-let* % false) (rest children))))
          :else (mapcat #(obsolete-let* % false) children))))

(defn obsolete-let [parsed-expressions]
  (map #(node->line % :warning :nested-let "obsolete let")
       (obsolete-let* parsed-expressions false)))

;;;; obsolete do

(defn obsolete-do* [{:keys [:children] :as rw-expr}
                    parent-do?]
  (let [current-do? (call? rw-expr 'do)]
    (cond (and current-do? (or parent-do?
                               (and (not= :unquote-splicing
                                          (:tag (second children)))
                                    (<= (count children) 2))))
          [rw-expr]
          :else (mapcat #(obsolete-do* % current-do?) children))))

(defn obsolete-do [parsed-expressions]
  (map #(node->line % :warning :obsolete-do "obsolete do")
       (obsolete-do* parsed-expressions false)))

;;;; processing of string input

(defn process-input
  [input]
  (let [;; workaround for https://github.com/xsc/rewrite-clj/issues/75
        input (-> input
                  (str/replace "##Inf" "::Inf")
                  (str/replace "##-Inf" "::-Inf")
                  (str/replace "##NaN" "::NaN"))
        parsed-expressions
        (p/parse-string-all input)
        parsed-expressions (remove-whitespace parsed-expressions)
        ids (inline-def parsed-expressions)
        nls (obsolete-let parsed-expressions)
        ods (obsolete-do parsed-expressions)]
    (concat ids nls ods)))

;;;; printing

(defn print-findings [file findings]
  (doseq [{:keys [:type :message :level :row :col]} findings]
    (println (str file ":" row ":" col ": " (name level) ": " message))))

(defn print-help []
  (println "Usage: --lint <file>. Use - for reading from stdin.")
  nil)

;;;; main

(defn -main [& [option file]]
  (when-let [[to-read filename]
             (case option
               "--lint" (if (= "-" file)
                          [*in* "<stdin>"]
                          [file file])
               "--help" (print-help)
               (print-help))]
    (let [input (slurp to-read)
          findings (process-input input)]
      (print-findings filename findings))))

;;;; scratch

(comment
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
  )
