(ns bar
  {:clj-kondo/config '{:hooks {foo/foo "

(require '[clj-kondo.hooks-api :as api])

(fn [{:keys [:node]}]
  {:node (api/list-node
    (list* (api/token-node 'inc)
           (rest (:children node))))})"}}}
  (:require [foo]))

(foo/foo "foo")
