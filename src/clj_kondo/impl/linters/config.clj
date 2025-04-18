(ns clj-kondo.impl.linters.config
  "Linting of clj-kondo's own configuration"
  {:no-doc true}
  (:require [clj-kondo.impl.config :refer [default-config]]
            [clj-kondo.impl.findings :as findings]
            [clj-kondo.impl.linters.edn-utils :as edn-utils]
            [clj-kondo.impl.utils :refer [sexpr node->line keyword-node? node->keyword string-from-token]]
            [clj-kondo.impl.version :as version]))

(set! *warn-on-reflection* true)

(def expected-linter-keys (set (keys (:linters default-config))))
(def expected-top-level-keys #{:min-clj-kondo-version})

(defn lint-map-vals [ctx node-map ks]
  (doseq [[kw-node val-node] node-map]
    (let [form (sexpr val-node)]
      (when (and (contains? ks (sexpr kw-node))
                 (not (map? form)))
        (findings/reg-finding!
         ctx
         (node->line (:filename ctx)
                     val-node
                     :clj-kondo-config
                     (str "Expected a map, but got: " (edn-utils/name-for-type form))))))))

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
  (lint-map-vals ctx node-map expected-linter-keys)
  (doseq [key-node (keys config-map)]
    (let [k (sexpr key-node)]
      (when (and
             (simple-keyword? k)
             (not (contains? expected-top-level-keys k))
             (contains? expected-linter-keys k))
        (findings/reg-finding!
         ctx
         (node->line (:filename ctx)
                     key-node
                     :clj-kondo-config
                     "Linter config should go under :linters"))))))

(defn ^:private compare-versions
  "Returns a finding message if the current version
   is below the minimum version"
  [{minimum :minimum
    current :current}]
  (let [earlier-version (fn
                          [v1 v2]
                          (first (sort [v1 v2])))]
    (when
     (not=
      minimum
      (earlier-version
       current
       minimum))
      (str
       "Version "
       current
       " below configured minimum "
       minimum))))

(defn min-clj-kondo-version-node? [n]
  (and (keyword-node? n)
       (= :min-clj-kondo-version (node->keyword n))))

(defn check-minimum-version
  "Registers a finding if the version is below the configured minimum"
  [ctx expr]
  (let [minimum-version (-> ctx :config :min-clj-kondo-version)
        warning (when minimum-version
                  (compare-versions {:minimum minimum-version
                                     :current version/version}))]
    (when warning
      (findings/reg-finding!
       ctx
       (if (seq expr)
         (node->line
          (:filename ctx)
          (let [version-value-node (second (drop-while (complement min-clj-kondo-version-node?) (:children expr)))]
            (if (= minimum-version (string-from-token version-value-node))
              version-value-node
              expr))
          :min-clj-kondo-version warning)
         {:message  warning
          :filename "<clj-kondo>"
          :type     :min-clj-kondo-version
          :row      1
          :col      1})))))

(defn lint-config [ctx expr]
  (check-minimum-version ctx expr)
  (try
    (let [config-edn (edn-utils/sexpr-keys expr)
          config-map (edn-utils/node-map expr)
          _ (lint-map-vals ctx config-edn #{:linters :lint-as :output :hooks})
          linters (:linters config-edn)
          linters-map (edn-utils/node-map linters)
          _ (lint-map-vals ctx linters-map expected-linter-keys)
          _ (lint-linters ctx linters-map config-map)
          _lint-as (:lint-as config-edn)]
      :eastwood)
    ;; Due to ubiquitous use of sexpr, we're catching coercion errors here and let them slide.
    (catch Exception e
      (binding [*out* *err*]
        (println "[clj-kondo] ERROR: " (.getMessage e))))))
