(ns clj-kondo.impl.linters.deps-edn
  (:require [clj-kondo.impl.findings :as findings]
            [clj-kondo.impl.utils :refer [sexpr node->line]]))

(defn sexpr-keys [map-node]
  (let [children (:children map-node)
        keys (take-nth 2 children)
        keys (map sexpr keys)
        vals (take-nth 2 (rest children))]
    (zipmap keys vals)))

(defn key-nodes [map-node]
  (take-nth 2 (:children map-node)))

(defn val-nodes [map-node]
  (take-nth 2 (rest (:children map-node))))


(defn lint-qualified-deps [ctx dep-map]
  (let [keys (key-nodes dep-map)]
    (run! (fn [node]
            (let [form (sexpr node)]
              (when-not (qualified-symbol? form)
                (findings/reg-finding!
                 ctx
                 (node->line (:filename ctx)
                             node
                             :warning
                             :deps.edn
                             (format "Libs must be qualified, change %s => %<s/%<s" form))))))
          keys)))

(defn lint-deps-edn [ctx expr]
  (let [deps-edn (sexpr-keys expr)
        _ (lint-qualified-deps ctx (:deps deps-edn))
        aliases (:aliases deps-edn)
        alias-maps (val-nodes aliases)
        alias-maps (map sexpr-keys alias-maps)
        extra-dep-maps (map :extra-deps alias-maps)]
    (run! #(lint-qualified-deps ctx %) extra-dep-maps)
    nil))
