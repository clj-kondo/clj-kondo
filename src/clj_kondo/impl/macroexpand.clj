(ns clj-kondo.impl.macroexpand
  (:require
   [clj-kondo.impl.utils :refer [some-call filter-children]]
   [clojure.walk :refer [prewalk]]
   [rewrite-clj.node.protocols :as node :refer [tag]]
   [rewrite-clj.node.seq :refer [vector-node list-node]]
   [rewrite-clj.node.token :refer [token-node]]))

;;;; macro expand

(defn expand-> [{:keys [:children] :as expr}]
  (let [children (rest children)]
    (loop [[child1 child2 & children :as all-children] children]
      (if child2
        (if (= :list (node/tag child2))
          (recur
           (let [res (into
                      [(with-meta
                         (list-node (reduce into
                                            [[(first (:children child2))]
                                             [child1] (rest (:children child2))]))
                         (meta child2))] children)]
             res))
          (recur (into [(with-meta (list-node [child2 child1])
                          (meta child2))] children)))
        child1))))

(defn find-fn-args [children]
  (filter-children #(and (= :token (tag %))
                         (:string-value %)
                         (re-matches #"%\d?\d?" (:string-value %)))
                   children))

(defn expand-fn [{:keys [:children] :as expr}]
  (let [fn-body (list-node children)
        args (find-fn-args children)
        arg-list (vector-node args)]
    (list-node [(token-node 'fn) arg-list fn-body])))

(defn expand-all [expr]
  (clojure.walk/prewalk
   #(if (:children %)
      (assoc % :children
             (map (fn [n]
                    (cond (some-call n ->)
                          (expand-> n)
                          (= :fn (node/tag n))
                          (expand-fn n)
                          :else n))
                  (:children %)))
      %)
   expr))

;;;; Scratch

(comment
  )
