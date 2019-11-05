(ns clj-kondo.impl.analyzer.compojure
  {:no-doc true}
  (:require
   [clj-kondo.impl.analyzer.common :refer [analyze-expression** extract-bindings
                                           ctx-with-bindings analyze-children]]
   [clj-kondo.impl.utils :as utils :refer [tag]]))

(defn normalize-compojure-vector [ctx expr]
  (loop [[fst & children] (:children expr)
         normalized-children []]
    (if fst
      (if (when-let [k (utils/node->keyword fst)]
            (identical? :<< k))
        (let [snd (first children)]
          ;; coercing function
          (analyze-expression** ctx snd)
          (recur (rest children)
                 normalized-children))
        (recur children (conj normalized-children fst)))
      (assoc expr :children normalized-children))))

(defn analyze-compojure-macro [ctx expr fn-name]
  (let [rfn? (= 'rfn fn-name)
        children (next (:children expr))
        children (if-not rfn? (do
                                ;; rfn doesn't have the routes string arg
                                (analyze-expression** ctx (first children))
                                (rest children))
                         children)
        destructuring-form (first children)
        vector? (when destructuring-form
                  (= :vector (tag destructuring-form)))
        destructuring-form (if vector?
                             (normalize-compojure-vector ctx destructuring-form)
                             destructuring-form)
        bindings (extract-bindings ctx destructuring-form)
        ctx (ctx-with-bindings ctx bindings)]
    (analyze-children ctx (next children))))
