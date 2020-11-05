(ns clj-kondo.impl.linters.deps-edn
  (:require [clj-kondo.impl.findings :as findings]
            [clj-kondo.impl.utils :refer [sexpr node->line]]))

(set! *warn-on-reflection* true)

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

(defn lint-qualified-deps [ctx nodes]
  (run! (fn [node]
          (let [form (sexpr node)]
            (when-not (qualified-symbol? form)
              (findings/reg-finding!
               ctx
               (node->line (:filename ctx)
                           node
                           :warning
                           :deps.edn
                           #_{:clj-kondo/ignore[:format]} ;; fixed on master
                           (format "Libs must be qualified, change %s => %<s/%<s" form))))))
        nodes))

(defn lint-dep-coordinates [ctx nodes]
  (run! (fn [node]
          (let [form (sexpr node)]
            (if-not (map? form)
              (findings/reg-finding!
               ctx
               (node->line (:filename ctx)
                           node
                           :warning
                           :deps.edn
                           (str "Expected map, found: " (.getName (class form)))))
              (do nil nil))))
        nodes))

(defn lint-deps-edn [ctx expr]
  (let [deps-edn (sexpr-keys expr)
        deps (:deps deps-edn)
        _ (lint-qualified-deps ctx (-> deps key-nodes))
        _ (lint-dep-coordinates ctx (-> deps val-nodes))
        aliases (:aliases deps-edn)
        alias-maps (val-nodes aliases)
        alias-maps (map sexpr-keys alias-maps)
        extra-dep-maps (map :extra-deps alias-maps)
        extra-dep-map-vals (mapcat val-nodes extra-dep-maps)
        _ (lint-dep-coordinates ctx extra-dep-map-vals)
        extra-dep-map-vals (map sexpr-keys extra-dep-map-vals)
        exclusions (map (comp :children :exclusions) extra-dep-map-vals)]
    (run! #(lint-qualified-deps ctx (key-nodes %)) extra-dep-maps)
    (run! #(lint-qualified-deps ctx %) exclusions)
    nil))
