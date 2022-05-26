(ns clj-kondo.impl.linters.edn-utils
  (:require [clj-kondo.impl.utils :refer [sexpr]]))

(set! *warn-on-reflection* true)

(defn sexpr-keys [map-node]
  (let [children (:children map-node)
        keys (take-nth 2 children)
        keys (map sexpr keys)
        vals (take-nth 2 (rest children))]
    (zipmap keys vals)))

(defn key-nodes [map-node]
  (if (identical? :namespaced-map (:tag map-node))
    (let [nspace-k (-> map-node :ns :k)
          map-node (first (:children map-node))
          knodes (take-nth 2 (:children map-node))]
      (map #(assoc % :namespace nspace-k) knodes))
    (take-nth 2 (:children map-node))))

(defn val-nodes [map-node]
  (if (identical? :namespaced-map (:tag map-node))
    (let [map-node (first (:children map-node))
          vnodes (take-nth 2 (rest (:children map-node)))]
      vnodes)
    (take-nth 2 (rest (:children map-node)))))

(defn name-for-type [form]
  (cond
    (nil? form) "nil"
    (symbol? form) "symbol"
    (int? form) "int"
    (float? form) "float"
    (keyword? form) "keyword"
    (char? form) "char"
    (string? form) "string"
    (map? form) "map"
    (vector? form) "vector"
    (list? form) "list"
    (set? form) "set"
    :else (.getName (class form))))

(defn node-map [raw-node]
  (zipmap (-> raw-node key-nodes)
          (-> raw-node val-nodes)))
