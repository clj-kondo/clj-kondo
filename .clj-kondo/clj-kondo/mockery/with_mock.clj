(ns hooks.mockery.with-mock
  (:require [clj-kondo.hooks-api :as api]))

(defn with-mock [{:keys [:node]}]
  (let [[mock map & body] (rest (:children node))]
    (try
      (when-not (symbol? (api/sexpr mock))
        (throw (ex-info "Mock is not a symbol" (meta mock))))
      (when-not (map? (api/sexpr map))
        (throw (ex-info "Mock binding is not a map" (meta map))))
      (if-let [target (:target (api/sexpr map))]
        (when-not (qualified-keyword? target)
          (throw (ex-info (str target " must be fully qualified") (meta map))))
        (throw (ex-info "no target specified" (meta map))))
      (when-not body
        (api/reg-finding! (assoc (meta (first (:children node)))
                                 :message "with-mock with empty body"
                                 :type :mockery)))
      {:node (api/list-node
              (list*
               (api/token-node 'let*)
               (api/vector-node [mock map])
               mock
               body))}
      (catch Exception e
        (api/reg-finding! (assoc (ex-data e) :message (ex-message e) :type :hook))
        ;; when there is an error, ignore macro and only return body (in vector to prevent redundant do)
        {:node (api/vector-node body)}))))
