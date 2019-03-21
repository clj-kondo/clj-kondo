(ns clj-kondo.core
  (:gen-class)
  (:require
   [clojure.string :as str]
   [clojure.walk :refer [prewalk]]
   [rewrite-clj.node.protocols :as node]
   [rewrite-clj.node.whitespace :refer [whitespace?]]
   [rewrite-clj.parser :as p]))

(defn uneval? [node]
  (= :uneval (node/tag node)))

(defn remove-whitespace [rw-expr]
  (clojure.walk/prewalk
   #(if (seq? %)
      (remove (fn [n]
                (or (whitespace? n)
                    (uneval? n))) %) %) rw-expr))

(defmacro call? [rw-expr & syms]
  `(and (= :list (:tag ~rw-expr))
        (~(set syms) (:value (first (:children ~rw-expr))))))

(defn node->line [node level type message]
  (let [m (meta node)]
    {:type type
     :message message
     :level level
     :row (:row m)
     :col (:col m)}))

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
  (let [implicit-do? (call? rw-expr 'fn 'defn 'defn-
                            'let 'loop 'binding 'with-open
                            'doseq)
        current-do? (call? rw-expr 'do)]
    (cond (and current-do? (or parent-do?
                               (and (not= :unquote-splicing
                                          (:tag (second children)))
                                    (<= (count children) 2))))
          [rw-expr]
          :else (mapcat #(obsolete-do* % (or implicit-do? current-do?)) children))))

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

(defn process-file [f]
  (if (= "-" f)
    (map #(assoc % :file "<stdin>") (process-input (slurp *in*)))
    (map #(assoc % :file f) (process-input (slurp f)))))

;;;; scratch

;;;; scratch

(comment
  ;; TODO: turn some of these into tests
  (inline-defs (p/parse-string-all "(defn foo []\n  (def x 1))"))
  (obsolete-let (p/parse-string-all "(let [i 10])"))
  (obsolete-do (p/parse-string-all "(do 1 (do 1 2))"))
  (process-input "(fn [] (do 1 2))")
  (process-input "(let [] 1 2 (do 1 2 3))")
  (process-input "(defn foo [] (do 1 2 3))")
  (process-input "(defn foo [] (fn [] 1 2 3))")
  )
