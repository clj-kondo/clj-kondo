(ns clj-kondo.impl.analyzer.test
  {:no-doc true}
  (:require
   [clj-kondo.impl.utils :as utils]))

(defn analyze-deftest [{:keys [:analyze-defn] :as ctx} _deftest-ns expr]
  (analyze-defn ctx
                (update expr :children
                        (fn [[_ name-expr & body]]
                          (list*
                           (utils/token-node 'clojure.core/defn)
                           name-expr
                           (utils/vector-node [])
                           body)))))

(defn analyze-cljs-test-async [{:keys [:analyze-children] :as ctx} expr]
  (let [[binding-expr & rest-children] (rest (:children expr))
        binding-name (:value binding-expr)
        ctx (utils/ctx-with-bindings ctx {binding-name (meta binding-expr)})
        ctx (assoc-in ctx [:arities binding-name]
                      {:fixed-arities #{0}})]
    (analyze-children ctx rest-children)))
