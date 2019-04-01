(ns clj-kondo.impl.macroexpand-test
  (:require
   [clj-kondo.impl.macroexpand :as macroexpand]
   [clj-kondo.impl.utils :refer [parse-string]]
   [clojure.test :as t :refer [deftest is testing]]
   [rewrite-clj.node.protocols :refer [tag]]))

(defn location [node]
  (let [m (meta node)]
    (when (and (:row m) (:col m))
      m)))

(deftest expand->-test
  (testing
      "Expanded -> expression preserves location"
    (is
     (every? location
             (filter #(= :list (tag %))
                     (tree-seq :children :children
                               (macroexpand/expand->
                                (parse-string "(-> 1 inc inc)"))))))))

(deftest expand-fn-test
  (testing
      "Expanded function literals have a location for the function they call"
      (is
       (every? location
               (filter #(= :list (tag %))
                       (tree-seq :children :children
                                 (macroexpand/expand-fn
                                  (parse-string "#(valid? %)"))))))))

