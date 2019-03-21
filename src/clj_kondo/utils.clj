(ns clj-kondo.utils
  (:require
   [clojure.walk :refer [prewalk]]
   [rewrite-clj.node.protocols :as node]
   [rewrite-clj.node.seq :refer [list-node]]
   [rewrite-clj.node.whitespace :refer [whitespace?]]
   [rewrite-clj.parser :as p]))

(defn uneval? [node]
  (= :uneval (node/tag node)))

(defn comment? [node]
  (= :comment (node/tag node)))

(defn remove-noise [rw-expr]
  (clojure.walk/prewalk
   #(if (:children %)
      (assoc % :children
             (remove (fn [n]
                       (or (whitespace? n)
                           (uneval? n)
                           (comment? n))) (:children %)))
      %) rw-expr))

(defmacro call? [rw-expr & syms]
  `(and (= :list (:tag ~rw-expr))
        (~(set syms) (:value (first (:children ~rw-expr))))))

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

(defn parse-string-all [s]
  (remove-noise (p/parse-string-all s)))
