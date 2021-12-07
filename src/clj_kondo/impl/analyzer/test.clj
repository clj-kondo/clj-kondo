(ns clj-kondo.impl.analyzer.test
  {:no-doc true}
  (:require
    [clj-kondo.impl.analyzer.common :as common]
    [clj-kondo.impl.macroexpand :as macros]
    [clj-kondo.impl.utils :as utils]))

(defn analyze-deftest [ctx expr defined-by resolved-as-ns resolved-as-name]
  (common/analyze-defn
   ctx
   (-> expr
       (update
        :children
        (fn [[_ name-expr & body]]
          (list*
           (utils/token-node 'clojure.core/defn)
           (when name-expr (vary-meta name-expr
                                      assoc
                                      :linted-as (symbol (str resolved-as-ns) (str resolved-as-name))
                                      :defined-by defined-by
                                      :test true))
           (utils/vector-node [])
           body))))
   defined-by))

(defn analyze-testing [ctx expr resolved-as-namespace]
  (let [[testing testing-str & rest-children] (:children expr)
        kns (keyword resolved-as-namespace)]
    (common/analyze-children (assoc-in ctx [:context kns :testing-str] (utils/sexpr testing-str))
                             testing)
    (common/analyze-children ctx rest-children)))

(defn analyze-cljs-test-async [ctx expr]
  (let [[binding-expr & rest-children] (rest (:children expr))
        binding-name (:value binding-expr)
        ctx (utils/ctx-with-bindings ctx {binding-name (meta binding-expr)})
        ctx (assoc-in ctx [:arities binding-name]
                      {:fixed-arities #{0}})]
    (common/analyze-children ctx rest-children)))

(defn analyze-are [ctx resolved-namespace expr]
  (let [[_ argv expr & args] (:children expr)
        is-expr (utils/list-node [(utils/token-node (symbol (str resolved-namespace) "is")) expr])
        new-node (macros/expand-do-template ctx
                                            (utils/list-node (list* nil
                                                                    argv
                                                                    is-expr
                                                                    args)))]
    (common/analyze-expression** ctx new-node)))
