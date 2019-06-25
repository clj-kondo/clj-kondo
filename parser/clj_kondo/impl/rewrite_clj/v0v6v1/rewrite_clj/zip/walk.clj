(ns ^{:no-doc true} clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.zip.walk
  (:require [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.custom-zipper.core :as z]
            [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.zip
             [subedit :refer [subedit-node]]
             [move :as m]]))

(defn- prewalk-subtree
  [p? f zloc]
  (loop [loc zloc]
    (if (m/end? loc)
      loc
      (if (p? loc)
        (if-let [n (f loc)]
          (recur (m/next n))
          (recur (m/next loc)))
        (recur (m/next loc))))))

(defn prewalk
  "Perform a depth-first pre-order traversal starting at the given zipper location
   and apply the given function to each child node. If a predicate `p?` is given,
   only apply the function to nodes matching it."
  ([zloc f] (prewalk zloc (constantly true) f))
  ([zloc p? f]
   (->> (partial prewalk-subtree p? f)
        (subedit-node zloc))))

(defn postwalk-subtree
  [p? f loc]
  (let [nloc (m/next loc)
        loc' (if (m/end? nloc)
               loc
               (m/prev (postwalk-subtree p? f nloc)))]
    (if (p? loc')
      (or (f loc') loc')
      loc')))

(defn ^{:added "0.4.9"} postwalk
  "Perform a depth-first post-order traversal starting at the given zipper location
   and apply the given function to each child node. If a predicate `p?` is given,
   only apply the function to nodes matching it."
  ([zloc f] (postwalk zloc (constantly true) f))
  ([zloc p? f]
   (subedit-node zloc #(postwalk-subtree p? f %))))
