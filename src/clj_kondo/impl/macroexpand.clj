(ns clj-kondo.impl.macroexpand
  {:no-doc true}
  (:require
   [clj-kondo.impl.utils :refer [some-call filter-children
                                 parse-string]]
   [clj-kondo.impl.metadata :refer [lift-meta]]
   [rewrite-clj.node.protocols :as node :refer [tag]]
   [rewrite-clj.node.seq :refer [vector-node list-node]]
   [rewrite-clj.node.token :refer [token-node]]
   [clj-kondo.impl.profiler :as profiler]))

(defn expand-> [filename expr]
  (profiler/profile
   :expand->
   (let [expr (lift-meta filename expr)
         children (:children expr)
         [c & cforms] (rest children)]
     (loop [x c, forms cforms]
       (if forms
         (let [form (first forms)
               threaded (if (= :list (node/tag form))
                          (with-meta (list-node (list* (first (:children form))
                                                       x
                                                       (next (:children form)))) (meta form))
                          (with-meta (list-node (list form x))
                            (meta form)))]
           (recur threaded (next forms)))
         x)))))

(defn expand->> [filename expr]
  (let [expr (lift-meta filename expr)
        children (:children expr)
        [c & cforms] (rest children)]
    (loop [x c, forms cforms]
      (if forms
        (let [form (first forms)
              threaded
              (if (= :list (node/tag form))
                (with-meta
                  (list-node
                   (concat
                    (cons (first (:children form))
                          (next (:children form)))
                    (list x)))
                  (meta form))
                (with-meta (list-node (list form x))
                  (meta form)))]
          (recur threaded (next forms)))
        x))))

(defn find-fn-args [children]
  (filter-children #(and (= :token (tag %))
                         (:string-value %)
                         (re-matches #"%\d?\d?" (:string-value %)))
                   children))

(defn expand-fn [{:keys [:children] :as expr}]
  (let [{:keys [:row :col] :as m} (meta expr)
        fn-body (with-meta (list-node children)
                  {:row row
                   :col (inc col)})
        args (find-fn-args children)
        arg-list (vector-node args)]
    (with-meta
      (list-node [(token-node 'fn) arg-list fn-body])
      m)))

;;;; Scratch

(comment
  (expand-fn (parse-string "#(inc ^long %)"))
  )
