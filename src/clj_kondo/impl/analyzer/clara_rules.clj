(ns clj-kondo.impl.analyzer.clara-rules
  {:no-doc true}
  (:require
    [clj-kondo.impl.analyzer.common :refer [extract-bindings
                                            ctx-with-bindings
                                            analyze-children]]
    [clj-kondo.impl.namespace :as namespace]
    [clj-kondo.impl.rewrite-clj.node.protocols :as node]
    [clojure.string :as str]))

(defn- node-value
  [a-node]
  (some-> a-node node/sexpr))

(defn- node-type
  [a-node]
  (some-> a-node node/tag))

(defn- binding-node?
  "determine if a symbol is a clara-rules binding symbol in the form `?<name>`"
  [node]
  (let [node-name (node-value node)]
    (when (and (symbol? node-name)
               (str/starts-with? (name node-name) "?"))
      node)))

(defn analyze-constraints
  "sequentially analyzes constraint expressions of clara rules and queries
  defined via defrule or defquery by sequentially analyzing its children lhs
  expressions and bindings."
  [context condition]
  (let [condition-args (if (= :vector (node-type (first condition))) (first condition) nil)
        condition-bindings (when condition-args
                             (extract-bindings context condition-args))
        local-context (ctx-with-bindings context condition-bindings)
        constraint-seq (if condition-args (rest condition) condition)]
    (loop [[constraint-expr & more] constraint-seq
           local-context local-context
           context context
           results []]
      (if (nil? constraint-expr)
        [results context]
        (let [constraint (:children constraint-expr)
              binding-form (when (= '= (node-value (first constraint)))
                             (some binding-node? (rest constraint)))
              constraint-bindings (when binding-form
                                    (extract-bindings local-context binding-form))
              local-context (ctx-with-bindings local-context constraint-bindings)
              context (ctx-with-bindings context constraint-bindings)
              next-results (analyze-children local-context constraint)]
          (recur more local-context context (concat results next-results)))))))

(defn analyze-conditions
  "sequentially analyzes condition expressions of clara rules and queries
  defined via defrule and defquery by taking into account the optional
  result binding, optional args bindings and sequentially analyzing
  its children constraint expressions."
  [context condition-seq]
  (loop [[condition-expr & more] condition-seq
         context context
         results []]
    (if (nil? condition-expr)
      [results context]
      (let [condition (:children condition-expr)
            result-form (when (= '<- (-> condition second node-value))
                          (first condition))
            [fact-node & condition] (if result-form (drop 2 condition) condition)
            fact-results (analyze-children context [fact-node])
            [next-results next-context] (cond
                                          (nil? condition)
                                          [[] context]

                                          (contains? #{:not :or :and :exists} (node-value fact-node))
                                          (analyze-conditions context condition)

                                          (and (= (node-type fact-node) :list)
                                               (= :from (-> condition first node-value)))
                                          (analyze-conditions context (rest condition))

                                          :else
                                          (analyze-constraints context condition))
            result-bindings (when result-form
                              (extract-bindings context result-form))
            next-context (ctx-with-bindings next-context result-bindings)]
        (recur more next-context (concat results fact-results next-results))))))

(defn analyze-defquery-macro
  "analyze clara-rules defquery macro"
  [context expr]
  (let [[name-node destructuring-form & condition-seq] (next (:children expr))
        query-bindings (when destructuring-form
                         (extract-bindings context destructuring-form))
        context (ctx-with-bindings context query-bindings)
        ns-name (-> context :ns :name)
        var-name (node-value name-node)]
    (when var-name
      (namespace/reg-var!
        context ns-name var-name expr {:temp true}))
    (let [[results] (analyze-conditions context condition-seq)]
      results)))

(defn analyze-defrule-macro
  "analyze clara-rules defrule macro"
  [context expr]
  (let [[name-node & production-seq] (next (:children expr))
        ns-name (-> context :ns :name)
        var-name (node-value name-node)
        [condition-seq _ body-seq] (partition-by (comp #{'=>} node-value) production-seq)]
    (when var-name
      (namespace/reg-var!
        context ns-name var-name expr {:temp true}))
    (let [[lhs-results context] (analyze-conditions context condition-seq)
          rhs-results (analyze-children context body-seq)]
      (concat lhs-results rhs-results))))

;;; scratch
(comment
  )
