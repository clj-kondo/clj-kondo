(ns ^{:no-doc true} clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.zip.edit
  (:refer-clojure :exclude [replace])
  (:require [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.zip
             [base :as base]
             [move :as m]
             [remove :as r]
             [whitespace :as ws]]
            [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.node :as node]
            [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.custom-zipper
             [core :as z]
             [utils :as u]]))

;; ## In-Place Modification

(defn replace
  "Replace the node at the given location with one representing
   the given value. (The value will be coerced to a node if
   possible.)"
  [zloc value]
  (z/replace zloc (node/coerce value)))

(defn- edit-node
  "Create s-expression from node, apply the function and create
   node from the result."
  [node f]
  (-> (node/sexpr node)
      (f)
      (node/coerce)))

(defn edit
  "Apply the given function to the s-expression at the given
   location, using its result to replace the node there. (The
   result will be coerced to a node if possible.)"
  [zloc f & args]
  (z/edit zloc edit-node #(apply f % args)))

;; ## Splice

(defn splice
  "Splice the given node, i.e. merge its children into the current one
   (akin to Clojure's `unquote-splicing` macro: `~@...`).

   - if the node is not one that can have children, no modification will
     be performed.
   - if the node has no or only whitespace children, it will be removed.
   - otherwise, splicing will be performed, moving the zipper to the first
     non-whitespace child afterwards.
   "
  [zloc]
  (if (z/branch? zloc)
    (if-let [children (->> (z/children zloc)
                           (drop-while node/whitespace?)
                           (reverse)
                           (drop-while node/whitespace?)
                           (seq))]
      (let [loc (->> (reduce z/insert-right zloc children)
                     (u/remove-and-move-right))]
        (or (ws/skip-whitespace loc) loc))
      (r/remove zloc))
    zloc))

;; ## Prefix/Suffix

(defn- edit-token
  [zloc str-fn]
  (let [e (base/sexpr zloc)
        e' (cond (string? e) (str-fn e)
                 (keyword? e) (keyword (namespace e) (str-fn (name e)))
                 (symbol? e) (symbol (namespace e) (str-fn (name e))))]
    (z/replace zloc (node/token-node e'))))

(defn- edit-multi-line
  [zloc line-fn]
  (let [n (-> (z/node zloc)
              (update-in [:lines] (comp line-fn vec)))]
    (z/replace zloc n)))

(defn prefix
  [zloc s]
  (case (base/tag zloc)
    :token      (edit-token zloc #(str s %))
    :multi-line (->> (fn [lines]
                       (if (empty? lines)
                         [s]
                         (update-in lines [0] #(str s %))))
                     (edit-multi-line zloc ))))

(defn suffix
  [zloc s]
  (case (base/tag zloc)
    :token      (edit-token zloc #(str % s))
    :multi-line (->> (fn [lines]
                       (if (empty? lines)
                         [s]
                         (concat (butlast lines) (str (last lines) s))))
                     (edit-multi-line zloc))))
