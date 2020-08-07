(ns hooks.rum.defc
  (:require [clj-kondo.hooks-api :as api]))

(defn defc [{:keys [:node]}]
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

(defn defcs
  [{:keys [node]}]
  (let [args (rest (:children node))
        component-name (first args)
        args (next args)
        body (loop [args* args
                    mixins []]
               (if (seq args*)
                 (let [a (first args*)]
                   (if (vector? (api/sexpr a))
                     (cons a (concat mixins (rest args*)))
                     (recur (rest args*)
                            (conj mixins a))))
                 args))
        partial-state (api/list-node [(api/token-node 'partial)
                                    (api/list-node (list* (api/token-node 'fn) body))
                                    (api/token-node 'state)])
        new-node (api/list-node
                  [(api/token-node 'let)
                   (api/vector-node
                    [(api/token-node 'state) (api/token-node {})])
                   (with-meta (api/list-node [(api/token-node 'def)
                                              component-name
                                              partial-state])
                     (meta node))])]
    {:node new-node}))
