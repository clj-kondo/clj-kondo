(ns
  ^{:no-doc true} clj-kondo.impl.rewrite-clj.node
  (:require [clj-kondo.impl.rewrite-clj.node
             coerce
             comment
             fn
             forms
             integer
             keyword
             meta
             protocols
             quote
             reader-macro
             regex
             seq
             string
             token
             uneval
             whitespace]
            [clj-kondo.impl.rewrite-clj.potemkin :refer [import-vars]]))

;; ## API Facade

(import-vars
  [clj-kondo.impl.rewrite-clj.node.protocols
   coerce
   children
   child-sexprs
   concat-strings
   inner?
   leader-length
   length
   printable-only?
   replace-children
   sexpr
   sexprs
   string
   tag]

  [clj-kondo.impl.rewrite-clj.node.comment
   comment-node
   comment?]

  [clj-kondo.impl.rewrite-clj.node.fn
   fn-node]

  [clj-kondo.impl.rewrite-clj.node.forms
   forms-node]

  [clj-kondo.impl.rewrite-clj.node.integer
   integer-node]

  [clj-kondo.impl.rewrite-clj.node.keyword
   keyword-node]

  [clj-kondo.impl.rewrite-clj.node.meta
   meta-node
   raw-meta-node]

  [clj-kondo.impl.rewrite-clj.node.regex
   regex-node]

  [clj-kondo.impl.rewrite-clj.node.reader-macro
   deref-node
   eval-node
   reader-macro-node
   var-node]

  [clj-kondo.impl.rewrite-clj.node.seq
   list-node
   map-node
   namespaced-map-node
   set-node
   vector-node]

  [clj-kondo.impl.rewrite-clj.node.string
   string-node]

  [clj-kondo.impl.rewrite-clj.node.quote
   quote-node
   syntax-quote-node
   unquote-node
   unquote-splicing-node]

  [clj-kondo.impl.rewrite-clj.node.token
   token-node]

  [clj-kondo.impl.rewrite-clj.node.uneval
   uneval-node]

  [clj-kondo.impl.rewrite-clj.node.whitespace
   comma-separated
   line-separated
   linebreak?
   newlines
   newline-node
   spaces
   whitespace-node
   whitespace?
   comma-node
   comma?
   whitespace-nodes])

;; ## Predicates

(defn whitespace-or-comment?
  "Check whether the given node represents whitespace or comment."
  [node]
  (or (whitespace? node)
      (comment? node)))

;; ## Value

(defn ^{:deprecated "0.4.0"} value
  "DEPRECATED: Get first child as a pair of tag/sexpr (if inner node),
   or just the node's own sexpr. (use explicit analysis of `children`
   `child-sexprs` instead) "
  [node]
  (if (inner? node)
    (some-> (children node)
            (first)
            ((juxt tag sexpr)))
    (sexpr node)))
