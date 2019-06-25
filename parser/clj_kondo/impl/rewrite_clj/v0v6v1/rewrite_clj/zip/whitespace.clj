(ns ^{:no-doc true} clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.zip.whitespace
  (:require [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.node :as node]
            [clj-kondo.impl.rewrite-clj.v0v6v1.rewrite-clj.custom-zipper.core :as z]))

;; ## Predicates

(defn whitespace?
  [zloc]
  (some-> zloc z/node node/whitespace?))

(defn linebreak?
  [zloc]
  (some-> zloc z/node node/linebreak?))

(defn whitespace-or-comment?
  [zloc]
  (some-> zloc z/node node/whitespace-or-comment?))

;; ## Movement

(defn skip
  "Perform the given movement while the given predicate returns true."
  [f p? zloc]
  (->> (iterate f zloc)
       (take-while identity)
       (take-while (complement z/end?))
       (drop-while p?)
       (first)))

(defn skip-whitespace
  "Perform the given movement (default: `z/right`) until a non-whitespace/
   non-comment node is encountered."
  ([zloc] (skip-whitespace z/right zloc))
  ([f zloc] (skip f whitespace-or-comment? zloc)))

(defn skip-whitespace-left
  "Move left until a non-whitespace/non-comment node is encountered."
  [zloc]
  (skip-whitespace z/left zloc))

;; ## Insertion

(defn ^{:added "0.5.0"} insert-space-left
  "Insert a whitespace node before the given one, representing the given
   number of spaces (default: 1)."
  ([zloc] (insert-space-left zloc 1))
  ([zloc n]
   {:pre [(>= n 0)]}
   (if (pos? n)
     (z/insert-left zloc (node/spaces n))
     zloc)))

(defn ^{:added "0.5.0"} insert-space-right
  "Insert a whitespace node after the given one, representing the given number
   of spaces (default: 1)."
  ([zloc] (insert-space-right zloc 1))
  ([zloc n]
   {:pre [(>= n 0)]}
   (if (pos? n)
     (z/insert-right zloc (node/spaces n))
     zloc)))

(defn ^{:added "0.5.0"} insert-newline-left
  "Insert a newline node before the given one, representing the given number of
   spaces (default: 1)."
  ([zloc] (insert-newline-left zloc 1))
  ([zloc n]
   (z/insert-left zloc (node/newlines n))))

(defn ^{:added "0.5.0"} insert-newline-right
  "Insert a newline node after the given one, representing the given number of
   linebreaks (default: 1)."
  ([zloc] (insert-newline-right zloc 1))
  ([zloc n]
   (z/insert-right zloc (node/newlines n))))

;; ## Deprecated Functions

(defn ^{:deprecated "0.5.0"} prepend-space
  "Prepend a whitespace node representing the given number of spaces (default: 1).

   DEPRECATED: use 'insert-space-left' instead."
  [zloc & [n]]
  (insert-space-left zloc (or n 1)))

(defn ^{:deprecated "0.5.0"} append-space
  "Append a whitespace node representing the given number of spaces (default: 1).

   DEPRECATED: use 'insert-space-right' instead."
  [zloc & [n]]
  (insert-space-right zloc (or n 1)))

(defn ^{:deprecated "0.5.0"} prepend-newline
  "Prepend a newline node representing the given number of linebreaks (default:
   1).

   DEPRECATED: use 'insert-newline-left' instead."
  [zloc & [n]]
  (insert-newline-left zloc (or n 1)))

(defn ^{:deprecated "0.5.0"} append-newline
  "Append a newline node representing the given number of linebreaks (default:
   1).

   DEPRECATED: use 'insert-newline-right' instead."
  [zloc & [n]]
  (insert-newline-right zloc (or n 1)))
