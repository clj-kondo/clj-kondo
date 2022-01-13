(ns clj-kondo.impl.linters.deps-edn
  "Linter for deps.edn and bb.edn file contents."
  (:require [clj-kondo.impl.findings :as findings]
            [clj-kondo.impl.linters.edn-utils :as edn-utils]
            [clj-kondo.impl.utils :refer [sexpr node->line]]
            [clojure.string :as str]))

(set! *warn-on-reflection* true)

(defn lint-paths-container [ctx node ]
  (let [form (sexpr node)]
    (when-not (vector? form)
      (findings/reg-finding!
       ctx
       (node->line (:filename ctx)
                   node
                   :deps.edn
                   (str "Expected vector, found: " (edn-utils/name-for-type form))))
      true)))

(defn lint-bb-edn-paths-elems [ctx node]
  (doseq [node (:children node)]
    (let [form (sexpr node)]
      (when-not (string? form)
        (findings/reg-finding!
         ctx
         (node->line (:filename ctx)
                     node
                     :deps.edn
                     (str "Expected string, found: " (edn-utils/name-for-type form))))))))

(defn lint-deps-edn-paths-elems [ctx node]
  (doseq [node (:children node)]
    (let [form (sexpr node)]
      (when-not (or (keyword? form) (string? form))
        (findings/reg-finding!
         ctx
         (node->line (:filename ctx)
                     node
                     :deps.edn
                     (str "Expected string or keyword, found: " (edn-utils/name-for-type form))))))))

(defn lint-bb-edn-paths [ctx node]
  (when (and node (not (lint-paths-container ctx node)))
    (lint-bb-edn-paths-elems ctx node)))

(defn lint-deps-edn-paths [ctx node]
  (when (and node (not (lint-paths-container ctx node)))
    (lint-deps-edn-paths-elems ctx node)))

(defn lint-qualified-lib [ctx node]
  (let [form (sexpr node)]
    (when-not (or (qualified-symbol? form)
                  ;; fix for namespaced maps
                  (:namespace node))
      (findings/reg-finding!
       ctx
       (node->line (:filename ctx)
                   node
                   :deps.edn
                   #_{:clj-kondo/ignore[:format]} ;; fixed on master
                   (format "Libs must be qualified, change %s => %<s/%<s" form)))
      true)))

(defn lint-qualified-deps [ctx nodes]
  (run! #(lint-qualified-lib ctx %) nodes))

(defn derive-git-url-from-lib [lib]
  (when lib
    (let [lib (str lib)]
      (when (or
             (str/starts-with? lib "io.")
             (str/starts-with? lib "com."))
        lib))))

(defn lint-dep-coord [ctx lib node]
  (let [form (sexpr node)
        git-url (or (:git/url form)
                    (derive-git-url-from-lib lib))]
    (if-not (map? form)
      (findings/reg-finding!
       ctx
       (node->line (:filename ctx)
                   node
                   :deps.edn
                   (str "Expected map, found: " (edn-utils/name-for-type form))))
      (or (when-let [version (:mvn/version form)]
            (when (or (= "RELEASE" version)
                      (= "LATEST" version))
              (findings/reg-finding!
               ctx
               (node->line (:filename ctx)
                           node
                           :deps.edn
                           (str "Non-determistic version."))))
            true)
          (when (:git/url form)
            (when (and (:git/sha form) (:sha form))
              (findings/reg-finding!
               ctx
               (node->line (:filename ctx)
                           node
                           :deps.edn
                           (str "Conflicting keys :git/sha and :sha."))))
            (when (and (:git/tag form) (:tag form))
              (findings/reg-finding!
               ctx
               (node->line (:filename ctx)
                           node
                           :deps.edn
                           (str "Conflicting keys :git/tag and :tag."))))
            (when-not (or (:git/sha form) (:sha form))
              (findings/reg-finding!
               ctx
               (node->line (:filename ctx)
                           node
                           :deps.edn
                           (str "Missing required key :git/sha."))))
            true)
          ;; no linting yet
          (:local/root form)

          ;; in this case git/url is inferred from lib
          ;; see https://clojure.org/reference/deps_and_cli#_coord_attributes
          (and git-url (:git/sha form))
          (prn :git-url git-url)
          ;; no condition met, generic warning
          (findings/reg-finding!
           ctx
           (node->line (:filename ctx)
                       node
                       :deps.edn
                       (str "Missing required key: :mvn/version, :git/url or :local/root.")))))))

(defn lint-deps [ctx kvs]
  (doseq [[lib coord] kvs]
    (let [error? (lint-qualified-lib ctx lib)]
      (lint-dep-coord ctx (when-not error? (sexpr lib)) coord))))

(defn lint-alias-keys [ctx nodes]
  (run! (fn [node]
          (let [form (sexpr node)]
            (if (not (keyword? form))
              (findings/reg-finding!
               ctx
               (node->line (:filename ctx)
                           node
                           :deps.edn
                           (str "Prefer keyword for alias.")))
              (when (contains? #{:deps :extra-deps :jvm-opts} form)
                (findings/reg-finding!
                 ctx
                 (node->line (:filename ctx)
                             node
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
                             :deps.edn
                             (str "JVM opts should be seqable of strings.")))))))
        alias-nodes))

(defn lint-mvn-repos [ctx mvn-repos]
  (let [repo-map-nodes (edn-utils/val-nodes mvn-repos)]
    (run! (fn [repo-map-node]
            (let [form (sexpr repo-map-node)]
              (when-not (and (map? form)
                             (:url form))
                (findings/reg-finding!
                 ctx
                 (node->line (:filename ctx)
                             repo-map-node
                             :deps.edn
                             (str "Expected: map with :url."))))))
          repo-map-nodes)))

(defn lint-bb-edn [ctx expr]
  (try
    (let [bb-edn (edn-utils/sexpr-keys expr)]
      (lint-bb-edn-paths ctx (:paths bb-edn))
      (lint-deps ctx (-> bb-edn :deps edn-utils/node-map)))
    ;; Due to ubiquitous use of sexpr, we're catching coercion errors here and let them slide.
    (catch Exception e
      (binding [*out* *err*]
        (println "ERROR: " (.getMessage e))))))

(defn lint-deps-edn [ctx expr]
  (try
    (let [deps-edn (edn-utils/sexpr-keys expr)
          _ (lint-deps-edn-paths ctx (:paths deps-edn))
          deps (:deps deps-edn)
          _ (lint-deps ctx (edn-utils/node-map deps))
          aliases (:aliases deps-edn)
          alias-keys (edn-utils/key-nodes aliases)
          _ (lint-alias-keys ctx alias-keys)
          alias-maps (edn-utils/val-nodes aliases)
          alias-maps (map edn-utils/sexpr-keys alias-maps)
          _ (lint-aliases ctx alias-maps)
          extra-dep-maps (map :extra-deps alias-maps)
          _ (run! #(lint-deps ctx (edn-utils/node-map %)) extra-dep-maps)
          extra-dep-map-vals (mapcat edn-utils/val-nodes extra-dep-maps)
          ;; _ (lint-dep-coordinates ctx extra-dep-map-vals)
          extra-dep-map-vals (map edn-utils/sexpr-keys extra-dep-map-vals)
          exclusions (map (comp :children :exclusions) extra-dep-map-vals)
          _ (run! #(lint-qualified-deps ctx %) exclusions)]
      (when-let [mvn-repos (:mvn/repos deps-edn)]
        (lint-mvn-repos ctx mvn-repos)))
    ;; Due to ubiquitous use of sexpr, we're catching coercion errors here and let them slide.
    (catch Exception e
      (binding [*out* *err*]
        (println "ERROR: " (.getMessage e))))))
