(ns hooks.claypoole
  (:refer-clojure :exclude [future pmap pvalues])
  (:require [clj-kondo.hooks-api :as api]))

(defn pool-and-body
  [token]
  (fn [{:keys [:node]}]
    (let [[_pool & body] (rest (:children node))]
      (let [new-node (api/list-node
                       (list*
                         (api/token-node token)
                         body))]
        {:node new-node}))))

(defn pool-with-binding-vec-or-exprs-and-body
  [token]
  (fn [{:keys [:node]}]
    (let [[_pool binding-vec-or-exprs & body] (rest (:children node))]
      (let [new-node (api/list-node
                       (list*
                         (api/token-node token)
                         binding-vec-or-exprs
                         body))]
        {:node new-node}))))

(def future (pool-and-body 'future))
(def completable-future (pool-and-body 'future))
(def pdoseq (pool-with-binding-vec-or-exprs-and-body 'doseq))
(def pmap (pool-and-body 'map))
(def pvalues (pool-and-body 'pvalues))
(def upvalues (pool-and-body 'pvalues))
(def pfor (pool-with-binding-vec-or-exprs-and-body 'for))
(def upfor (pool-with-binding-vec-or-exprs-and-body 'for))