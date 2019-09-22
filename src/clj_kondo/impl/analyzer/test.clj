(ns clj-kondo.impl.analyzer.test
  {:no-doc true})

(in-ns 'clj-kondo.impl.analyzer)

;; defined in clj-kondo.impl.analyzer
(declare extract-bindings ctx-with-bindings
         analyze-children analyze-defn)

(defn analyze-deftest [ctx _deftest-ns expr]
  (analyze-defn ctx
                (update expr :children
                        (fn [[_ name-expr & body]]
                          (list*
                           (utils/token-node 'clojure.core/defn)
                           name-expr
                           (utils/vector-node [])
                           body)))))

(defn analyze-cljs-test-async [ctx expr]
  (let [[binding-expr & rest-children] (rest (:children expr))
        binding-name (:value binding-expr)
        ctx (ctx-with-bindings ctx {binding-name (meta binding-expr)})
        ctx (assoc-in ctx [:arities binding-name]
                      {:fixed-arities #{0}})]
    (analyze-children ctx rest-children)))
