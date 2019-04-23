(ns clj-kondo.impl.macroexpand
  {:no-doc true}
  (:require
   [clj-kondo.impl.utils :refer [some-call filter-children]]
   [rewrite-clj.node.protocols :as node :refer [tag]]
   [rewrite-clj.node.seq :refer [vector-node list-node]]
   [rewrite-clj.node.token :refer [token-node]]))

(defn expand-> [{:keys [:children] :as expr}]
  ;; TODO: rewrite to zipper
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
  ;; TODO: rewrite to zipper
  (let [{:keys [:row :col] :as m} (meta expr)
        fn-body (with-meta (list-node children)
                  {:row row
                   :col (inc col)})
        args (find-fn-args children)
        arg-list (vector-node args)]
    (with-meta
      (list-node [(token-node 'fn*) arg-list fn-body])
      m)))

;;;; Scratch

(comment
  )
