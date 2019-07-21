(ns clj-kondo.impl.namespace
  {:no-doc true}
  (:require
   [clj-kondo.impl.config :as config]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.linters.misc :refer [lint-duplicate-requires!]]
   [clj-kondo.impl.utils :refer [node->line deep-merge linter-disabled?]]
   [clj-kondo.impl.var-info :as var-info]
   [clojure.string :as str]))

(set! *warn-on-reflection* true)

(defn reg-namespace!
  "Registers namespace. Deep-merges with already registered namespaces
  with the same name. Returns updated namespace."
  [{:keys [:base-lang :lang :namespaces]} ns]
  (let [path [base-lang lang (:name ns)]]
    (get-in (swap! namespaces update-in
                   path deep-merge ns)
            path)))

(defn reg-var!
  ([ctx ns-sym var-sym expr]
   (reg-var! ctx ns-sym var-sym expr nil))
  ([{:keys [:base-lang :lang :filename :findings :namespaces :top-level?]}
    ns-sym var-sym expr metadata]
   (let [metadata (assoc metadata
                         :ns ns-sym
                         :name var-sym)
         path [base-lang lang ns-sym]]
     (swap! namespaces update-in path
            (fn [ns]
              ;; declare is idempotent
              (when (and top-level? (not (:declared metadata)))
                (let [vars (:vars ns)]
                  (when-let [redefined-ns
                             (or (when-let [meta-v (get vars var-sym)]
                                   (when-not (or
                                              (:temp meta-v)
                                              (:declared meta-v))
                                     ns-sym))
                                 (when-let [qv (get (:referred-vars ns) var-sym)]
                                   (:ns qv))
                                 (let [core-ns (case lang
                                                 :clj 'clojure.core
                                                 :cljs 'cljs.core)]
                                   (when (and (not= ns-sym core-ns)
                                              (not (contains? (:clojure-excluded ns) var-sym))
                                              (var-info/core-sym? lang var-sym))
                                     core-ns)))]
                    (findings/reg-finding!
                     findings
                     (node->line filename
                                 expr :warning
                                 :redefined-var
                                 (if (= ns-sym redefined-ns)
                                   (str "redefined var #'" redefined-ns "/" var-sym)
                                   (str var-sym " already refers to #'" redefined-ns "/" var-sym)))))))
              (update ns :vars assoc
                      var-sym
                      metadata))))))

(defn reg-var-usage!
  [{:keys [:base-lang :lang :namespaces] :as ctx}
   ns-sym usage]
  (let [path [base-lang lang ns-sym]
        usage (assoc usage
                     :invalid-arity-disabled? (linter-disabled? ctx :invalid-arity)
                     :unresolved-symbol-disabled?
                     ;; TODO: can we do this via the ctx only?
                     (or (:unresolved-symbol-disabled? usage)
                         (linter-disabled? ctx :unresolved-symbol)))]
    (swap! namespaces update-in path
           (fn [ns]
             (update ns :used-vars conj
                     usage)))))

(defn reg-usage!
  "Registers usage of required namespaced in ns."
  [{:keys [:base-lang :lang :namespaces]} ns-sym required-ns-sym]
  (swap! namespaces update-in [base-lang lang ns-sym :used]
         conj required-ns-sym))

(defn reg-alias!
  [{:keys [:base-lang :lang :namespaces]} ns-sym alias-sym aliased-ns-sym]
  (swap! namespaces assoc-in [base-lang lang ns-sym :qualify-ns alias-sym]
         aliased-ns-sym))

(defn reg-binding!
  [{:keys [:base-lang :lang :namespaces]} ns-sym binding]
  (when-not (:clj-kondo/mark-used binding)
    (swap! namespaces update-in [base-lang lang ns-sym :bindings]
           conj binding))
  nil)

(defn reg-used-binding!
  [{:keys [:base-lang :lang :namespaces]} ns-sym binding]
  (swap! namespaces update-in [base-lang lang ns-sym :used-bindings]
         conj binding)
  nil)

(defn reg-required-namespaces!
  [{:keys [:base-lang :lang :namespaces] :as ctx} ns-sym analyzed-require-clauses]
  (swap! namespaces update-in [base-lang lang ns-sym]
         (fn [ns]
           (lint-duplicate-requires! ctx (:required ns) (:required analyzed-require-clauses))
           (merge-with into ns analyzed-require-clauses)))
  nil)

(defn java-class? [s]
  (let [splits (str/split s #"\.")]
    (and (> (count splits) 2)
         (Character/isUpperCase ^char (first (last splits))))))

(defn reg-unresolved-symbol!
  [{:keys [:namespaces :filename] :as ctx}
   ns-sym symbol {:keys [:base-lang :lang] :as loc}]
  ;; suppress fake let* resolve
  (when (:row loc)
    (let [filename (or filename (:filename loc))]
      (when-not (or (:unresolved-symbol-disabled? loc)
                    (config/unresolved-symbol-excluded
                     (or (:config loc)
                         (:config ctx)) (:callstack loc) symbol)
                    (let [symbol-name (name symbol)]
                      (or (str/starts-with? symbol-name
                                            ".")
                          (str/ends-with? symbol-name
                                          ".")
                          (java-class? symbol-name))))
        (swap! namespaces update-in [base-lang lang ns-sym :unresolved-symbols symbol]
               (fn [old-loc]
                 (if (nil? old-loc)
                   (assoc loc
                          :filename filename
                          :name symbol)
                   old-loc))))))
  nil)

(defn reg-used-referred-var!
  [{:keys [:base-lang :lang :namespaces] :as _ctx}
   ns-sym var]
  (swap! namespaces update-in [base-lang lang ns-sym :used-referred-vars]
         conj var))

(defn list-namespaces [{:keys [:namespaces]}]
  (for [[_base-lang m] @namespaces
        [_lang nss] m
        [_ns-name ns] nss]
    ns))

(defn get-namespace [{:keys [:namespaces]} base-lang lang ns-sym]
  (get-in @namespaces [base-lang lang ns-sym]))

(defn resolve-name
  [ctx ns-name name-sym]
  (let [lang (:lang ctx)
        ns (get-namespace ctx (:base-lang ctx) lang ns-name)]
    (if-let [ns* (namespace name-sym)]
      (let [ns-sym (symbol ns*)]
        (or (if-let [ns* (or (get (:qualify-ns ns) ns-sym)
                             ;; referring to the namespace we're in
                             (when (= (:name ns) ns-sym)
                               ns-sym))]
              {:ns ns*
               :name (symbol (name name-sym))}
              (when (= :clj lang)
                (when-let [ns* (or (get var-info/default-import->qname ns-sym)
                                   (get var-info/default-fq-imports ns-sym)
                                   (get (:java-imports ns) ns-sym))]
                  {:java-interop? true
                   :ns ns*
                   :name (symbol (name name-sym))})))))
      (or
       (when-let [[k v] (find (:referred-vars ns)
                              name-sym)]
         (reg-used-referred-var! ctx ns-name k)
         v)
       (when (contains? (:vars ns) name-sym)
         {:ns (:name ns)
          :name name-sym})
       (when-let [java-class (or (get var-info/default-import->qname name-sym)
                                 (get var-info/default-fq-imports name-sym)
                                 (get (:java-imports ns) name-sym))]
         {:ns java-class
          :java-interop? true
          :name name-sym})
       (let [clojure-excluded? (contains? (:clojure-excluded ns)
                                          name-sym)
             namespace (:name ns)
             core-sym? (when-not clojure-excluded?
                         (var-info/core-sym? lang name-sym))
             special-form? (contains? var-info/special-forms name-sym)]
         (if (or core-sym? special-form?)
           {:ns (case lang
                  :clj 'clojure.core
                  :cljs 'cljs.core)
            :name name-sym}
           {:ns namespace
            :name name-sym
            :unqualified? true
            :clojure-excluded? clojure-excluded?}))))))

;;;; Scratch

(comment
  )
