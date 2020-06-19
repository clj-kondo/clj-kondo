(ns bar
  {:clj-kondo/config '{:hooks {re-frame.core/dispatch "

(require '[clj-kondo.hooks-api :as api])

(fn [{:keys [:node]}]
  (let [sexpr (api/sexpr node)
        event (second sexpr)]
    (when-not (vector? event)
      (throw (ex-info \"dispatch arg should be vector!\"
               (or (meta (second (:children node))) {}))))
    (when-not (qualified-keyword? (first event))
      (throw (ex-info \"keyword should be fully qualified!\"
               (or (some-> node :children second :children first meta) {}))))))"}}}
  (:require [re-frame.core :as r :refer [dispatch]]))

(dispatch 1)
(dispatch [:foo 1])






