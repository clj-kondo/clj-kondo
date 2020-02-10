(ns clj-kondo.impl.analyzer.test
  {:no-doc true}
  (:require
    [clj-kondo.impl.analyzer.common :as common]
    [clj-kondo.impl.utils :as utils]))

(defn analyze-deftest [ctx deftest-ns expr]
  (common/analyze-defn
   (assoc ctx :test true)
   (-> expr
       (update
        :children
        (fn [[_ name-expr & body]]
          (list*
           (utils/token-node 'clojure.core/defn)
           (when name-expr (vary-meta name-expr assoc
                                      :defined-by (symbol (str deftest-ns) "deftest")
                                      :test true))
           (utils/vector-node [])
           body))))))

(defn analyze-cljs-test-async [ctx expr]
  (let [[binding-expr & rest-children] (rest (:children expr))
        binding-name (:value binding-expr)
        ctx (utils/ctx-with-bindings ctx {binding-name (meta binding-expr)})
        ctx (assoc-in ctx [:arities binding-name]
                      {:fixed-arities #{0}})]
    (common/analyze-children ctx rest-children)))

(defn analyze-are [ctx expr]
  (let [children (next (:children expr))
        symbols-node (first children)
        bindings (when symbols-node
                   (common/extract-bindings ctx symbols-node))
        ctx (common/ctx-with-bindings ctx bindings)]
    (common/analyze-children ctx (rest children))))
