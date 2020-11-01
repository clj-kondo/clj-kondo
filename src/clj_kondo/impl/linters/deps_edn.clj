(ns clj-kondo.impl.linters.deps-edn
  (:require [clj-kondo.impl.findings :as findings]
            [clj-kondo.impl.utils :refer [sexpr node->line]]))

(defn sexpr-keys [map-node]
  (let [children (:children map-node)
        keys (take-nth 2 children)
        keys (map sexpr keys)
        vals (take-nth 2 (rest children))]
    (zipmap keys vals)))

(defn lint-qualified-deps [ctx key-nodes]
  (run! (fn [node]
          (let [form (sexpr node)]
            (when-not (qualified-symbol? form)
              (findings/reg-finding! ctx
                                     (node->line (:filename ctx)
                                                 node
                                                 :warning
                                                 :deps.edn
                                                 (format "Libs must be qualified, change %s => %<s/%<s" form))))))
        key-nodes))

(defn lint-deps-edn [ctx expr]
  (let [deps-edn (sexpr-keys expr)
        deps (:deps deps-edn)
        dep-keys (take-nth 2 (:children deps))
        _ (lint-qualified-deps ctx dep-keys)]
    nil))

