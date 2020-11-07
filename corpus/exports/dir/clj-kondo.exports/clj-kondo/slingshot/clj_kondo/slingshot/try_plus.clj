(ns clj-kondo.slingshot.try-plus
  (:require [clj-kondo.hooks-api :as api]))

(defn expand-catch [catch-node]
  (let [[catch catchee & exprs] (:children catch-node)
        catchee-sexpr (api/sexpr catchee)]
    (cond (vector? catchee-sexpr)
          (let [[selector & exprs] exprs]
            (api/list-node
             [catch (api/token-node 'Exception) (api/token-node '_e#)
              (api/list-node
               (list* (api/token-node 'let)
                      (api/vector-node [selector (api/token-node nil)])
                      exprs))]))
          :else catch-node)))

(defn try+ [{:keys [node]}]
  (let [children (rest (:children node))
        [body catches]
        (loop [body children
               body-exprs []
               catches []]
          (if (seq body)
            (let [f (first body)
                  f-sexpr (api/sexpr f)]
              (if (and (seq? f-sexpr) (= 'catch (first f-sexpr)))
                (recur (rest body)
                       body-exprs
                       (conj catches (expand-catch f)))
                (recur (rest body)
                       (conj body-exprs f)
                       catches)))
            [body-exprs catches]))
        new-node (api/list-node
                  [(api/token-node 'let)
                   (api/vector-node
                    [(api/token-node '&throw-context) (api/token-node nil)])
                   (api/token-node '&throw-context) ;; use throw-context to avoid warning
                   (with-meta (api/list-node (list* (api/token-node 'try)
                                                    (concat body catches)))
                     (meta node))])]
    ;; (prn (api/sexpr new-node))
    {:node new-node}))

