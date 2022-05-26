(ns hooks.better-cond
  (:refer-clojure :exclude [cond]))

(require '[clj-kondo.hooks-api :as api])

(defn process-pairs [pairs]
  (loop [[[lhs rhs :as pair] & pairs] pairs
         new-body [(api/token-node 'cond)]]
    (if pair
      (let [lhs-sexpr (api/sexpr lhs)]
        (clojure.core/cond
          (= 1 (count pair)) (api/list-node (conj new-body lhs))
          (not (keyword? lhs-sexpr))
          (recur pairs
                 (conj new-body lhs rhs))
          (= :let lhs-sexpr)
          (api/list-node (conj new-body (api/token-node :else) (api/list-node [(api/token-node 'let) rhs (process-pairs pairs)])))))
      (api/list-node new-body))))

(def cond
  (fn [{:keys [:node]}]
    (let [expr (let [args (rest (:children node))
                     pairs (partition-all 2 args)]
                 (process-pairs pairs))]
      {:node (with-meta expr
               (meta node))})))
