(ns ^{:no-doc true} clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.zip.remove
  (:refer-clojure :exclude [remove])
  (:require [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.zip
             [move :as m]
             [whitespace :as ws]]
            [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.custom-zipper
             [core :as z]
             [utils :as u]]))

(defn- remove-trailing-space
  "Remove all whitespace following a given node."
  [zloc]
  (u/remove-right-while zloc ws/whitespace?))

(defn- remove-preceding-space
  "Remove all whitespace preceding a given node."
  [zloc]
  (u/remove-left-while zloc ws/whitespace?))

(defn remove
  "Remove value at the given zipper location. Returns the first non-whitespace
   node that would have preceded it in a depth-first walk. Will remove whitespace
   appropriately.

       [1  2  3]   => [1  3]
       [1 2]       => [1]
       [1 2]       => [2]
       [1]         => []
       [  1  ]     => []
       [1 [2 3] 4] => [1 [2 3]]
       [1 [2 3] 4] => [[2 3] 4]

   If a node is located rightmost, both preceding and trailing spaces are removed,
   otherwise only trailing spaces are touched. This means that a following element
   (no matter whether on the same line or not) will end up in the same position
   (line/column) as the removed one, _unless_ a comment lies between the original
   node and the neighbour."
  [zloc]
  {:pre [zloc]
   :post [%]}
  (->> (-> (if (or (m/rightmost? zloc)
                   (m/leftmost? zloc))
             (remove-preceding-space zloc)
             zloc)
           (remove-trailing-space)
           z/remove)
       (ws/skip-whitespace z/prev)))
