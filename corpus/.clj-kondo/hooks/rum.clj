(ns hooks.rum
  (:require [clj-kondo.hooks-api :as api]))

(defn f [{:keys [:node]}]
  (let [args (rest (:children node))
        component-name (first args)
        args (next args)
        body
        (loop [args* args
               mixins []]
          (if (seq args*)
            (let [a (first args*)]
              (if (vector? (api/sexpr a))
                (cons a (concat mixins (rest args*)))
                (recur (rest args*)
                       (conj mixins a))))
            args))
        new-node (with-meta
                   (api/list-node (list* (api/token-node 'defn) component-name body))
                   (meta node))]
    ;; (prn (meta sexpr))
    ;; (prn expr)
    {:node new-node}))
