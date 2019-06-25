(ns clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.zip
  (:refer-clojure :exclude [next find replace remove
                            seq? map? vector? list? set?
                            print map get assoc])
  (:require [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.zip
             [base :as base]
             [edit :as edit]
             [find :as find]
             [insert :as insert]
             [move :as move]
             [remove :as remove]
             [seq :as seq]
             [subedit :as subedit]
             [walk :as walk]
             [whitespace :as ws]]
            [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.potemkin :refer [import-vars]]
            [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj
             [parser :as p]
             [node :as node]]
            [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.custom-zipper.core :as z]))

;; ## API Facade

(import-vars
  [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.custom-zipper.core
   node position root]

  [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.zip.base
   child-sexprs
   edn* edn tag sexpr
   length value
   of-file of-string
   string root-string
   print print-root]

  [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.zip.edit
   replace edit splice
   prefix suffix]

  [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.zip.find
   find find-next
   find-depth-first
   find-next-depth-first
   find-tag find-next-tag
   find-value find-next-value
   find-token find-next-token]

  [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.zip.insert
   insert-right insert-left
   insert-child append-child]

  [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.zip.move
   left right up down prev next
   leftmost rightmost
   leftmost? rightmost? end?]

  [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.zip.remove
   remove]

  [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.zip.seq
   seq? list? vector? set? map?
   map map-keys map-vals
   get assoc]

  [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.zip.subedit
   edit-node edit-> edit->>
   subedit-node subedit-> subedit->>]

  [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.zip.walk
   prewalk
   postwalk]

  [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.zip.whitespace
   whitespace? linebreak?
   whitespace-or-comment?
   skip skip-whitespace
   skip-whitespace-left
   prepend-space append-space
   prepend-newline append-newline])

;; ## Base Operations

(defmacro ^:private defbase
  [sym base]
  (let [{:keys [arglists]} (meta
                             (ns-resolve
                               (symbol (namespace base))
                               (symbol (name base))))
        sym (with-meta
              sym
              {:doc (format "Directly call '%s' on the given arguments." base)
               :arglists `(quote ~arglists)})]
    `(def ~sym ~base)))

(defbase right* clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.custom-zipper.core/right)
(defbase left* clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.custom-zipper.core/left)
(defbase up* clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.custom-zipper.core/up)
(defbase down* clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.custom-zipper.core/down)
(defbase next* clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.custom-zipper.core/next)
(defbase prev* clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.custom-zipper.core/prev)
(defbase rightmost* clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.custom-zipper.core/rightmost)
(defbase leftmost* clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.custom-zipper.core/leftmost)
(defbase replace* clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.custom-zipper.core/replace)
(defbase edit* clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.custom-zipper.core/edit)
(defbase remove* clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.custom-zipper.core/remove)
(defbase insert-left* clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.custom-zipper.core/insert-left)
(defbase insert-right* clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.custom-zipper.core/insert-right)

;; ## DEPRECATED

(defn ^{:deprecated "0.4.0"} ->string
  "DEPRECATED. Use `string` instead."
  [zloc]
  (string zloc))

(defn ^{:deprecated "0.4.0"} ->root-string
  "DEPRECATED. Use `root-string` instead."
  [zloc]
  (root-string zloc))
