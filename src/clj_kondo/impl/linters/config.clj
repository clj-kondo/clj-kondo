(ns clj-kondo.impl.linters.config
  "Linting of clj-kondo's own configuration"
  (:require [clj-kondo.impl.config :refer [default-config]]
            [clj-kondo.impl.findings :as findings]
            [clj-kondo.impl.linters.edn-utils :as edn-utils]
            [clj-kondo.impl.utils :refer [sexpr node->line]]))

(def expected-linter-keys (set (keys (:linters default-config))))

(defn lint-linters [ctx node-map config-map]
  (doseq [key-node (keys node-map)]
    (let [k (sexpr key-node)]
      (when (and
             (simple-keyword? k)
             (not (contains? expected-linter-keys k)))
        (findings/reg-finding!
         ctx
         (node->line (:filename ctx)
                     key-node
                     :clj-kondo-config
                     (str "Unexpected linter name: " k))))))
  (doseq [key-node (keys config-map)]
    (let [k (sexpr key-node)]
      (when (and
             (simple-keyword? k)
             (contains? expected-linter-keys k))
        (findings/reg-finding!
         ctx
         (node->line (:filename ctx)
                     key-node
                     :clj-kondo-config
                     (str "Linter config should go under :linters")))))))

(defn lint-config [ctx expr]
  (try
    (let [config-edn (edn-utils/sexpr-keys expr)
          config-map (edn-utils/node-map expr)
          linters (:linters config-edn)
          linters-map (edn-utils/node-map linters)
          _ (lint-linters ctx linters-map config-map)
          _lint-as (:lint-as config-edn)]
      :eastwood)
    ;; Due to ubiquitous use of sexpr, we're catching coercion errors here and let them slide.
    (catch Exception e
      (binding [*out* *err*]
        (println "[clj-kondo] ERROR: " (.getMessage e))))))
