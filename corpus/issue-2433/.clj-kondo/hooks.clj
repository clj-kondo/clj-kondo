(ns hooks
  (:require [clj-kondo.hooks-api :as api]))

(defn dingo
  "Let's pretend we have `(dingo some-sym some-body)` macro that is expands to (let [some-sym 42] some-body)"
  [{:keys [node]}]
  (let [[somesym & somebody] (rest (:children node))
        new-node (api/list-node
                  (list*
                   (api/token-node `let)
                   (api/vector-node [somesym (api/token-node 42)])
                   somebody))]
    {:node new-node}))
