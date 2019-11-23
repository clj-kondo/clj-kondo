(ns clj-kondo.impl.namespace
  {:no-doc true}
  (:refer-clojure :exclude [ns-name])
  (:require
   [clj-kondo.impl.analysis :as analysis]
   [clj-kondo.impl.config :as config]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.linters.misc :refer [lint-duplicate-requires!]]
   [clj-kondo.impl.utils :refer [node->line deep-merge linter-disabled?]]
   [clj-kondo.impl.var-info :as var-info]
   [clojure.string :as str])
  (:import [java.util StringTokenizer]))

(set! *warn-on-reflection* true)

(defn reg-namespace!
  "Registers namespace. Deep-merges with already registered namespaces
  with the same name. Returns updated namespace."
  [{:keys [:base-lang :lang :namespaces]} ns]
  (let [{ns-name :name} ns
        path [base-lang lang ns-name]]
    (get-in (swap! namespaces update-in
                   path deep-merge ns)
            path)))

(defn reg-var!
  ([ctx ns-sym var-sym expr]
   (reg-var! ctx ns-sym var-sym expr nil))
  ([{:keys [:base-lang :lang :filename :findings :namespaces :top-level? :top-ns] :as ctx}
    ns-sym var-sym expr metadata]
   (let [{expr-row :row expr-col :col} (meta expr)
         metadata (assoc metadata
                         :ns ns-sym
                         :name var-sym
                         :row expr-row
                         :col expr-col)
         path [base-lang lang ns-sym]
         temp? (:temp metadata)
         config (:config ctx)]
     (when (and (-> config :output :analysis)
                (not temp?))
       (analysis/reg-var! ctx filename expr-row expr-col
                          ns-sym var-sym
                          metadata))
     (swap! namespaces update-in path
            (fn [ns]
              (let [vars (:vars ns)
                    prev-var (get vars var-sym)
                    prev-declared? (:declared prev-var)]
                ;; declare is idempotent
                (when (and top-level? (not (:declared metadata)))
                  (when-let [redefined-ns
                             (or (when-let [meta-v prev-var]
                                   (when-not (or
                                              (:temp meta-v)
                                              prev-declared?)
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
                                   (str var-sym " already refers to #'" redefined-ns "/" var-sym)))))
                  (when (and (not= :off (-> config :linters :missing-docstring :level))
                             (not (:private metadata))
                             (not (:doc metadata))
                             (not temp?))
                    (findings/reg-finding!
                     findings
                     (node->line filename
                                 expr :warning
                                 :missing-docstring
                                 "Missing docstring."))))
                (update ns :vars assoc
                        var-sym
                        (assoc
                         (merge metadata (select-keys prev-var [:row :col]))
                         :top-ns top-ns))))))))

(defn reg-var-usage!
  [{:keys [:base-lang :lang :namespaces] :as ctx}
   ns-sym usage]
  (let [path [base-lang lang ns-sym]
        usage (assoc usage
                     :config (:config ctx)
                     :unresolved-symbol-disabled?
                     ;; TODO: can we do this via the ctx only?
                     (or (:unresolved-symbol-disabled? usage)
                         (linter-disabled? ctx :unresolved-symbol)))]
    (swap! namespaces update-in path
           (fn [ns]
             (update ns :used-vars conj
                     usage)))))

(defn reg-used-namespace!
  "Registers usage of required namespaced in ns."
  [{:keys [:base-lang :lang :namespaces]} ns-sym required-ns-sym]
  (swap! namespaces update-in [base-lang lang ns-sym :used-namespaces]
         conj required-ns-sym))

(defn reg-proxied-namespaces!
  [{:keys [:base-lang :lang :namespaces]} ns-sym proxied-ns-syms]
  (swap! namespaces update-in [base-lang lang ns-sym :proxied-namespaces]
         into proxied-ns-syms))

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

(defn reg-imports!
  [{:keys [:base-lang :lang :namespaces] :as _ctx} ns-sym imports]
  (swap! namespaces update-in [base-lang lang ns-sym]
         (fn [ns]
           ;; TODO:
           ;; (lint-duplicate-imports! ctx (:required ns) ...)
           (update ns :imports merge imports)))
  nil)

(defn java-class? [s]
  (let [splits (str/split s #"\.")]
    (and (> (count splits) 2)
         (Character/isUpperCase ^char (first (last splits))))))

(defn reg-unresolved-symbol!
  [{:keys [:namespaces] :as _ctx}
   ns-sym symbol {:keys [:base-lang :lang :config
                         :callstack] :as sym-info}]
  (when-not (or (:unresolved-symbol-disabled? sym-info)
                (config/unresolved-symbol-excluded config
                                                   callstack symbol)
                (let [symbol-name (name symbol)]
                  (or (str/starts-with? symbol-name ".")
                      (java-class? symbol-name))))
    (swap! namespaces update-in [base-lang lang ns-sym :unresolved-symbols symbol]
           (fn [old-sym-info]
             (if (nil? old-sym-info)
               sym-info
               old-sym-info))))
  nil)

(defn reg-used-referred-var!
  [{:keys [:base-lang :lang :namespaces] :as _ctx}
   ns-sym var]
  (swap! namespaces update-in [base-lang lang ns-sym :used-referred-vars]
         conj var))

(defn reg-referred-all-var!
  [{:keys [:base-lang :lang :namespaces] :as _ctx}
   ns-sym referred-all-ns-sym var-sym]
  (swap! namespaces update-in [base-lang lang ns-sym :refer-alls referred-all-ns-sym :referred]
         conj var-sym))

(defn list-namespaces [{:keys [:namespaces]}]
  (for [[_base-lang m] @namespaces
        [_lang nss] m
        [_ns-name ns] nss]
    ns))

(defn reg-used-import!
  [{:keys [:base-lang :lang :namespaces] :as _ctx}
   ns-sym import]
  (prn "import" import)
  (swap! namespaces update-in [base-lang lang ns-sym :used-imports]
         conj import))

(defn get-namespace [{:keys [:namespaces]} base-lang lang ns-sym]
  (get-in @namespaces [base-lang lang ns-sym]))

(defn next-token [^StringTokenizer st]
  (when (.hasMoreTokens st)
    (.nextToken st)))

(defn first-segment
  "Returns first segment dot-delimited string, only if there is at least
  one part after the first dot."
  [name-sym]
  (let [st (StringTokenizer. (str name-sym) ".")]
    (when-let [ft (next-token st)]
      (symbol ft))))

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
                                   (get (:imports ns) ns-sym))]
                  (prn ">")
                  (reg-used-import! ctx ns-name ns*)
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
       (when-let [[name-sym* package]
                  (or (find var-info/default-import->qname name-sym)
                      (when-let [v (get var-info/default-fq-imports name-sym)]
                        [v v])
                      ;; (find (:imports ns) name-sym)
                      (if (identical? :cljs lang)
                        ;; CLJS allows imported classes to be used like this: UtcDateTime.fromTimestamp
                        ;; hmm, this causes the extractor to fuck up
                        (let [fs (first-segment name-sym)]
                          (find (:imports ns) fs))
                        (find (:imports ns) name-sym)))]
         (prn ">>")
         (reg-used-import! ctx ns-name package)
         ;; (prn "package" name-sym* name-sym '-> package)
         {:ns package
          :java-interop? true
          :name name-sym*})
       (when (= :cljs lang)
         (when-let [ns* (get (:qualify-ns ns) name-sym)]
           (when (some-> (meta ns*) :raw-name string?)
             {:ns ns*
              :name name-sym})))
       (let [clojure-excluded? (contains? (:clojure-excluded ns)
                                          name-sym)
             core-sym? (when-not clojure-excluded?
                         (var-info/core-sym? lang name-sym))
             special-form? (or (special-symbol? name-sym)
                               (contains? var-info/special-forms name-sym))]
         (if (or core-sym? special-form?)
           {:ns (case lang
                  :clj 'clojure.core
                  :cljs 'cljs.core)
            :name name-sym}
           (let [referred-all-ns (some (fn [[k {:keys [:excluded]}]]
                                         (when-not (contains? excluded name-sym)
                                           k))
                                       (:refer-alls ns))]
             {:ns (or referred-all-ns :clj-kondo/unknown-namespace)
              :name name-sym
              :unresolved? true
              :clojure-excluded? clojure-excluded?})))))))

;;;; Scratch

(comment
  )
