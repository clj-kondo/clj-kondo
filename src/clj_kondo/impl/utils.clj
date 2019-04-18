(ns clj-kondo.impl.utils
  {:no-doc true}
  (:require
   [clojure.walk :refer [prewalk]]
   [rewrite-clj.node.protocols :as node]
   [rewrite-clj.node.whitespace :refer [whitespace?]]
   [rewrite-clj.parser :as p]
   [clojure.string :as str]))

(defn tag [maybe-expr]
  (when maybe-expr
    (node/tag maybe-expr)))

(defn uneval? [node]
  (= :uneval (tag node)))

(defn comment? [node]
  (= :comment (tag node)))

(defmacro some-call
  "Determines if expr is a call to some symbol. Returns symbol if so."
  [expr & syms]
  (let [syms (set syms)]
    `(and (= :list (tag ~expr))
          ((quote ~syms) (:value (first (:children ~expr)))))))

(defn process-reader-conditional [node lang]
  ;; TODO: support :default
  (let [tokens (-> node :children last :children)]
    (loop [[k v & ts] tokens]
      (if (= lang (:k k))
        v
        (when (seq ts) (recur ts))))))

(comment
  n
  (process-reader-conditional n :clj))

(defn remove-noise
  ([expr] (remove-noise expr nil))
  ([expr config]
   (clojure.walk/postwalk
    #(if-let [children (:children %)]
       (let [new-children
             (seq (keep (fn [node]
                          (when-not
                              (or (whitespace? node)
                                  (uneval? node)
                                  (comment? node)
                                  (when (-> config :skip-comments)
                                    (some-call node comment core/comment)))
                            node))
                        children))]
         (if new-children
           (assoc % :children
                  new-children)
           (dissoc % :children)))
       %) expr)))

(defn select-lang
  ([expr lang]
   (clojure.walk/postwalk
    #(if-let [children (:children %)]
       (let [new-children
             (seq (reduce (fn [acc node]
                            (if (= :reader-macro
                                   (and node (node/tag node)))
                              (if-let [processed (process-reader-conditional node lang)]
                                (if (= "?@" (-> node :children first :string-value))
                                  (into acc (:children  processed))
                                  (conj acc processed))
                                acc)
                              (conj acc node)))
                          []
                          children))]
         (if new-children
           (assoc % :children
                  new-children)
           (dissoc % :children)))
       %) expr)))

(comment
  (select-lang (remove-noise (p/parse-string-all "[#?(:clj [1 2 3] )]"))
               :clj)
  (select-lang (remove-noise (p/parse-string-all "[#?@(:clj [1 2 3] )]"))
               :clj)


  (parse-string "#?(:clj [1 2 3])")
  )

(defn node->line [filename node level type message]
  (let [m (meta node)]
    {:type type
     :message message
     :level level
     :row (:row m)
     :col (:col m)
     :filename filename}))

(defn parse-string [s]
  (remove-noise (p/parse-string s)))

(defn parse-string-all
  ([s] (parse-string-all s nil))
  ([s config]
   (remove-noise (p/parse-string-all s) config)))

(defn filter-children
  "Recursively filters children by pred"
  [pred children]
  (mapcat #(if (pred %)
             [%]
             (if-let [cchildren (:children %)]
               (filter-children pred cchildren)
               []))
          children))

;;;; Scratch

(comment
  )
