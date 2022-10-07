(ns location
  {:clj-kondo/config '{:hooks {:analyze-call {hooks-location-test/foo "

(require '[clj-kondo.hooks-api :as api])

(fn [{:keys [:node]}]
  {:node (api/list-node
    (list* (api/token-node 'inc)
           (rest (:children node))))})"}}}}
  (:require [hooks-location-test :as foo]))

(foo/foo "foo")
(foo/foo 1 2 3)
