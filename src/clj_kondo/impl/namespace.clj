(ns clj-kondo.impl.namespace
  {:no-doc true}
  (:require
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.utils :refer [node->line parse-string
                                 parse-string-all deep-merge one-of]]
   [clj-kondo.impl.var-info :as var-info]
   [clojure.string :as str]
   [clj-kondo.impl.config :as config]))

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
  ([{:keys [:base-lang :lang :filename :findings :namespaces]} ns-sym var-sym expr metadata]
   (let [path [base-lang lang ns-sym]]
     (swap! namespaces update-in path
            (fn [ns]
              ;; declare is idempotent
              (when-not (:declared metadata)
                (let [vars (:vars ns)]
                  (when-let [redefined-ns
                             (or (when-let [v (get vars var-sym)]
                                   (when-not (:declared (meta v))
                                     ns-sym))
                                 (when-let [qv (get (:qualify-var ns) var-sym)]
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
              (update ns :vars conj (with-meta var-sym
                                      metadata)))))))

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
  (swap! namespaces update-in [base-lang lang ns-sym :bindings]
         conj binding)
  nil)

(defn reg-used-binding!
  [{:keys [:base-lang :lang :namespaces]} ns-sym binding]
  (swap! namespaces update-in [base-lang lang ns-sym :used-bindings]
         conj binding)
  nil)

(defn java-class? [s]
  (let [splits (str/split s #"\.")]
    (and (> (count splits) 2)
         (Character/isUpperCase ^char (first (last splits))))))

(defn reg-unresolved-symbol!
  [{:keys [:base-lang :lang :namespaces :filename :skip-unresolved?] :as ctx}
   ns-sym symbol loc]
  (when-not (or skip-unresolved?
                (config/unresolved-symbol-excluded ctx symbol)
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
               old-loc))))
  nil)

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
        (if-let [ns* (or (get (:qualify-ns ns) ns-sym)
                         ;; referring to the namespace we're in
                         (when (= (:name ns) ns-sym)
                           ns-sym))]
          {:ns ns*
           :name (symbol (name name-sym))}
          (when (= :clj lang)
            (when-let [ns* (or (get var-info/default-import->qname ns-sym)
                               (get var-info/default-fq-imports ns-sym))]
              {:java-interop? true
               :ns ns*
               :name (symbol (name name-sym))}))))
      (or
       (get (:qualify-var ns)
            name-sym)
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
