(ns clj-kondo.utils
  "Somehow moving the predicate which uses the protocol to another
  namespace helps preventing the error: Discovered unresolved type
  during parsing: rewrite_clj.node.protocols.Node. I don't know
  why..."
  (:require [rewrite-clj.node.protocols :as node]
            [rewrite-clj.node.whitespace :refer [whitespace?]]
            [clojure.walk :refer [prewalk]]))

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

;;;; scratch

(comment
  (call? {:tag :list :children [{:value 'def}]} 'def 'defn)
  (call? {:tag :list :children [{:value 'defn}]} 'def 'defn)
  )
