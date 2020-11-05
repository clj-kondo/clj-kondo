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
              (or (when-let [version (:mvn/version form)]
                    (when (or (= "RELEASE" version)
                              (= "LATEST" version))
                      (findings/reg-finding!
                       ctx
                       (node->line (:filename ctx)
                                   node
                                   :warning
                                   :deps.edn
                                   (str "Non-determistic version."))))
                    true)
                  (and (:git/url form)
                       (when-not (:sha form)
                         (findings/reg-finding!
                          ctx
                          (node->line (:filename ctx)
                                      node
                                      :warning
                                      :deps.edn
                                      (str "Missing required key :sha.")))))
                  (:local/root form)
                  (findings/reg-finding!
                   ctx
                   (node->line (:filename ctx)
                               node
                               :warning
                               :deps.edn
                               (str "Missing required key: :mvn/version, :git/url or :local/root.")))))))
        nodes))

(defn lint-alias-keys [ctx nodes]
  (run! (fn [node]
          (let [form (sexpr node)]
            (if (not (keyword? form))
              (findings/reg-finding!
               ctx
               (node->line (:filename ctx)
                           node
                           :warning
                           :deps.edn
                           (str "Prefer keyword for alias.")))
              (when (contains? #{:deps :extra-deps :jvm-opts} form)
                (findings/reg-finding!
                 ctx
                 (node->line (:filename ctx)
                             node
                             :warning
                             :deps.edn
                             (str "Suspicious alias name: " (name form))))))))
        nodes))

(defn lint-aliases [ctx alias-nodes]
  (run! (fn [alias-node]
          (when-let [jvm-opts-node (:jvm-opts alias-node)]
            (let [jvm-opts-form (sexpr jvm-opts-node)]
              (when (not (and (sequential? jvm-opts-form)
                              (every? string? jvm-opts-form)))
                (findings/reg-finding!
                 ctx
                 (node->line (:filename ctx)
                             jvm-opts-node
                             :warning
                             :deps.edn
                             (str "JVM opts should be seqable of strings.")))))))
        alias-nodes))

(defn lint-deps-edn [ctx expr]
  (let [deps-edn (sexpr-keys expr)
        deps (:deps deps-edn)
        _ (lint-qualified-deps ctx (-> deps key-nodes))
        _ (lint-dep-coordinates ctx (-> deps val-nodes))
        aliases (:aliases deps-edn)
        alias-keys (key-nodes aliases)
        _ (lint-alias-keys ctx alias-keys)
        alias-maps (val-nodes aliases)
        alias-maps (map sexpr-keys alias-maps)
        _ (lint-aliases ctx alias-maps)
        extra-dep-maps (map :extra-deps alias-maps)
        extra-dep-map-vals (mapcat val-nodes extra-dep-maps)
        _ (lint-dep-coordinates ctx extra-dep-map-vals)
        extra-dep-map-vals (map sexpr-keys extra-dep-map-vals)
        exclusions (map (comp :children :exclusions) extra-dep-map-vals)]
    (run! #(lint-qualified-deps ctx (key-nodes %)) extra-dep-maps)
    (run! #(lint-qualified-deps ctx %) exclusions)
    nil))
