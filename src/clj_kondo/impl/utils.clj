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

(defn remove-noise
  ([expr] (remove-noise expr nil))
  ([expr config]
   (clojure.walk/prewalk
    #(if-let [children (:children %)]
       (assoc % :children
              (remove (fn [n]
                        (or (whitespace? n)
                            (uneval? n)
                            (comment? n)
                            (when (:ignore-comments? config)
                              (some-call n comment core/comment)))) children))
       %) expr)))

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
