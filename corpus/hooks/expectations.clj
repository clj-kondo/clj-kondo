(ns hooks.expectations
  {:clj-kondo/config '{:hooks {:analyze-call {expectations.clojure.test/more-of "

(require '[clj-kondo.hooks-api :as api])

(fn [{:keys [:node]}]
  (let [children (rest (:children node))]
    {:node (api/list-node (list* (api/token-node 'let)
                                 (api/vector-node
                                  [(first children) (api/token-node nil)])
                                 (rest children)))}))

"}}}}
  (:require [expectations.clojure.test :as t]))

(t/expecting "numeric behavior"
             (t/expect (t/more-of {:keys [a b]} ;; bindings are recognized
                                  even? a
                                  odd?  b)
                       {:a (* 2 13) :b (* 3 13)})
             (t/expect pos? (* -3 -5)))

(t/expecting "numeric behavior"
             (t/expect (t/more-of {:keys [a b]} ;; unused binding b
                                  even? a
                                  odd?  b') ;; unresolved binding b'
                       {:a (* 2 13) :b (* 3 13)})
             (t/expect pos? (* -3 -5)))
