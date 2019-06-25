;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

;functional hierarchical zipper, with navigation, editing and enumeration
;see Huet

(ns ^{:no-doc true} clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.custom-zipper.core
  (:refer-clojure :exclude (replace remove next))
  (:require [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.node.protocols :as node]
            [clojure.zip :as clj-zip]))

;; ## Switch
;;
;; To not force users into using this custom zipper, the following flag
;; is used to dispatch to `clojure.zip` when set to `false`.

(defn ^:no-doc custom-zipper
  [root]
  {::custom? true
   :node     root
   :position [1 1]
   :parent   nil
   :left     []
   :right   '()})

(defn ^:no-doc zipper
  [root]
  (clj-zip/zipper
    node/inner?
    (comp seq node/children)
    node/replace-children
    root))

(defn ^:no-doc custom-zipper?
  [value]
  (::custom? value))

(defmacro ^:private defn-switchable
  [sym docstring params & body]
  (let [placeholders (repeatedly (count params) gensym)]
    `(defn ~sym
       ~docstring
       [~@placeholders]
       (if (custom-zipper? ~(first placeholders))
         (let [~@(interleave params placeholders)]
           ~@body)
         (~(symbol "clojure.zip" (name sym)) ~@placeholders)))))

;; ## Implementation

(defn-switchable node
  "Returns the node at loc"
  [{:keys [node]}]
  node)

(defn-switchable branch?
  "Returns true if the node at loc is a branch"
  [{:keys [node]}]
  (node/inner? node))

(defn-switchable children
  "Returns a seq of the children of node at loc, which must be a branch"
  [{:keys [node] :as loc}]
  (if (branch? loc)
    (seq (node/children node))
    (throw (Exception. "called children on a leaf node"))))

(defn-switchable ^:no-doc make-node
  "Returns a new branch node, given an existing node and new
  children. The loc is only used to supply the constructor."
  [loc node children]
  (node/replace-children node children))

(defn position
  "Returns the ones-based [row col] of the start of the current node"
  [loc]
  (if (custom-zipper? loc)
    (:position loc)
    (throw
      (IllegalStateException.
        (str
          "to use the 'position' function, please construct your zipper with "
           "':track-position?'  set to true.")))))

(defn-switchable lefts
  "Returns a seq of the left siblings of this loc"
  [loc]
  (map first (:left loc)))

(defn-switchable down
  "Returns the loc of the leftmost child of the node at this loc, or
  nil if no children"
  [loc]
  (when (branch? loc)
    (let [{:keys [node path] [row col] :position} loc
          [c & cnext :as cs] (children loc)]
      (when cs
        {::custom? true
         :node     c
         :position [row (+ col (node/leader-length node))]
         :parent   loc
         :left     []
         :right    cnext}))))

(defn-switchable up
  "Returns the loc of the parent of the node at this loc, or nil if at
  the top"
  [loc]
  (let [{:keys [node parent left right changed?]} loc]
    (when parent
      (if changed?
        (assoc parent
               :changed? true
               :node (make-node loc
                                (:node parent)
                                (concat (map first left) (cons node right))))
        parent))))

(defn-switchable root
  "zips all the way up and returns the root node, reflecting any changes."
  [{:keys [end?] :as loc}]
  (if end?
    (node loc)
    (let [p (up loc)]
      (if p
        (recur p)
        (node loc)))))

(defn-switchable right
  "Returns the loc of the right sibling of the node at this loc, or nil"
  [loc]
  (let [{:keys [node parent position left] [r & rnext :as right] :right} loc]
    (when (and parent right)
      (assoc loc
             :node r
             :left (conj left [node position])
             :right rnext
             :position (node/+extent position (node/extent node))))))

(defn-switchable rightmost
  "Returns the loc of the rightmost sibling of the node at this loc, or self"
  [loc]
  (if-let [next (right loc)]
    (recur next)
    loc))

(defn-switchable left
  "Returns the loc of the left sibling of the node at this loc, or nil"
  [loc]
  (let [{:keys [node parent left right]} loc]
    (when (and parent (seq left))
      (let [[lnode lpos] (peek left)]
        (assoc loc
               :node lnode
               :position lpos
               :left (pop left)
               :right (cons node right))))))

(defn-switchable leftmost
  "Returns the loc of the leftmost sibling of the node at this loc, or self"
  [loc]
  (let [{:keys [node parent left right]} loc]
    (if (and parent (seq left))
      (let [[lnode lpos] (first left)]
        (assoc loc
               :node lnode
               :position lpos
               :left []
               :right (concat (map first (rest left)) [node] right)))
      loc)))

(defn-switchable insert-left
  "Inserts the item as the left sibling of the node at this loc,
 without moving"
  [loc item]
  (let [{:keys [parent position left]} loc]
    (if-not parent
      (throw (new Exception "Insert at top"))
      (assoc loc
             :changed? true
             :left (conj left [item position])
             :position (node/+extent position (node/extent item))))))

(defn-switchable insert-right
  "Inserts the item as the right sibling of the node at this loc,
  without moving"
  [loc item]
  (let [{:keys [parent right]} loc]
    (if-not parent
      (throw (new Exception "Insert at top"))
      (assoc loc
             :changed? true
             :right (cons item right)))))

(defn-switchable replace
  "Replaces the node at this loc, without moving"
  [loc node]
  (assoc loc :changed? true :node node))

(defn edit
  "Replaces the node at this loc with the value of (f node args)"
  [loc f & args]
  (if (custom-zipper? loc)
    (replace loc (apply f (node loc) args))
    (apply clj-zip/edit loc f args)))

(defn-switchable insert-child
  "Inserts the item as the leftmost child of the node at this loc,
  without moving"
  [loc item]
  (replace loc (make-node loc (node loc) (cons item (children loc)))))

(defn-switchable append-child
  "Inserts the item as the rightmost child of the node at this loc,
  without moving"
  [loc item]
  (replace loc (make-node loc (node loc) (concat (children loc) [item]))))

(defn-switchable next
  "Moves to the next loc in the hierarchy, depth-first. When reaching
  the end, returns a distinguished loc detectable via end?. If already
  at the end, stays there."
  [{:keys [end?] :as loc}]
  (if end?
    loc
    (or
     (and (branch? loc) (down loc))
     (right loc)
     (loop [p loc]
       (if (up p)
         (or (right (up p)) (recur (up p)))
         (assoc p :end? true))))))

(defn-switchable prev
  "Moves to the previous loc in the hierarchy, depth-first. If already
  at the root, returns nil."
  [loc]
  (if-let [lloc (left loc)]
    (loop [loc lloc]
      (if-let [child (and (branch? loc) (down loc))]
        (recur (rightmost child))
        loc))
    (up loc)))

(defn-switchable end?
  "Returns true if loc represents the end of a depth-first walk"
  [loc]
  (:end? loc))

(defn-switchable remove
  "Removes the node at loc, returning the loc that would have preceded
  it in a depth-first walk."
  [loc]
  (let [{:keys [node parent left right]} loc]
    (if-not parent
      (throw (new Exception "Remove at top"))
      (if (seq left)
        (loop [loc (let [[lnode lpos] (peek left)]
                     (assoc loc
                            :changed? true
                            :position lpos
                            :node lnode
                            :left (pop left)))]
          (if-let [child (and (branch? loc) (down loc))]
            (recur (rightmost child))
            loc))
        (assoc parent
               :changed? true
               :node (make-node loc (:node parent) right))))))
