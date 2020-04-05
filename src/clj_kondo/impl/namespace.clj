(ns clj-kondo.impl.namespace
  {:no-doc true}
  (:refer-clojure :exclude [ns-name])
  (:require
   [clj-kondo.impl.analysis :as analysis]
   [clj-kondo.impl.config :as config]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.utils :refer [node->line deep-merge linter-disabled? one-of]]
   [clj-kondo.impl.var-info :as var-info]
   [clojure.string :as str])
  (:import [java.util StringTokenizer]))

(set! *warn-on-reflection* true)

(defn lint-duplicate-requires!
  ([ctx namespaces] (lint-duplicate-requires! ctx #{} namespaces))
  ([ctx init namespaces]
   (reduce (fn [required ns]
             (if (contains? required ns)
               (do
                 (findings/reg-finding!
                   ctx
                   (node->line (:filename ctx)
                               ns
                               :warning
                               :duplicate-require
                               (str "duplicate require of " ns)))
                 required)
               (conj required ns)))
           (set init)
           namespaces)
   nil))

(defn lint-conflicting-aliases! [ctx namespaces]
  (let [config (:config ctx)
        level (-> config :linters :conflicting-alias :level)]
    (when-not (identical? :off level)
      (loop [aliases #{}
             ns-maps (filter :as namespaces)]
        (let [{:keys [ns as]} (first ns-maps)]
          (when (contains? aliases as)
            (findings/reg-finding!
              ctx
              (node->line (:filename ctx)
                          as
                          :warning
                          :conflicting-alias
                          (str "Conflicting alias for " ns))))
          (when (seq (rest ns-maps))
            (recur (conj aliases as)
                   (rest ns-maps))))))))

(defn lint-unsorted-required-namespaces! [ctx namespaces]
  (let [config (:config ctx)
        level (-> config :linters :unsorted-required-namespaces :level)]
    (when-not (identical? :off level)
      (loop [last-processed-ns nil
             ns-list namespaces]
        (when ns-list
          (let [ns (first ns-list)
                m (meta ns)
                raw-ns (:raw-name m)
                ns-str (str (or raw-ns ns))
                branch (:branch m)]
            (cond branch
                  (recur last-processed-ns (next ns-list))
                  (pos? (compare last-processed-ns ns-str))
                  (findings/reg-finding!
                   ctx
                   (node->line (:filename ctx)
                               ns
                               level
                               :unsorted-required-namespaces
                               (str "Unsorted namespace: " ns)))
                  :else (recur ns-str
                               (next ns-list)))))))))

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
  ([{:keys [:base-lang :lang :filename :namespaces :top-level? :top-ns] :as ctx}
    ns-sym var-sym expr metadata]
   (let [m (meta expr)
         expr-row (:row m)
         expr-col (:col m)
         expr-end-row (:end-row m)
         expr-end-col (:end-col m)
         metadata (assoc metadata
                         :ns ns-sym
                         :name var-sym
                         :name-row (:row metadata)
                         :name-col (:col metadata)
                         :name-end-row (:end-row metadata)
                         :name-end-col (:end-col metadata)
                         :row expr-row
                         :col expr-col
                         :end-row expr-end-row
                         :end-col expr-end-col)
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
                     ctx
                     (node->line filename
                                 expr :warning
                                 :redefined-var
                                 (if (= ns-sym redefined-ns)
                                   (str "redefined var #'" redefined-ns "/" var-sym)
                                   (str var-sym " already refers to #'" redefined-ns "/" var-sym)))))
                  (when (and (not (identical? :off (-> config :linters :missing-docstring :level)))
                             (not (:private metadata))
                             (not (:doc metadata))
                             (not (:test metadata))
                             (not temp?)
                             (not
                              (when-let [linted-as (:linted-as metadata)]
                                (or
                                 (= 'clojure.test/deftest linted-as)
                                 (= 'clojure.core/deftype linted-as)
                                 (= 'clojure.core/defrecord linted-as)
                                 (= 'clojure.core/defprotocol linted-as)
                                 (= 'clojure.core/definterface linted-as)))))
                    (findings/reg-finding!
                     ctx
                     (node->line filename
                                 expr :warning
                                 :missing-docstring
                                 "Missing docstring."))))
                (update ns :vars assoc
                        var-sym
                        (assoc
                         (merge metadata (select-keys
                                          prev-var
                                          [:row :col :end-row :end-col]))
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

(defn reg-destructuring-default!
  [{:keys [:base-lang :lang :namespaces :ns]} default binding]
  (swap! namespaces
         update-in [base-lang lang (:name ns) :destructuring-defaults]
         conj (assoc default :binding binding))
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
           (lint-conflicting-aliases! ctx (:required analyzed-require-clauses))
           (lint-unsorted-required-namespaces! ctx (:required analyzed-require-clauses))
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

(defn class-name? [s]
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
                      (class-name? symbol-name))))
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
  ;; (prn "import" import)
  (swap! namespaces update-in [base-lang lang ns-sym :used-imports]
         conj import))

(defn reg-unresolved-namespace!
  [{:keys [:base-lang :lang :namespaces :config :callstack :filename] :as _ctx} ns-sym unresolved-ns]
  ;; NOTE: we check the unresolved-symbol config to exclude certain macros, most
  ;; notably user/defproject, but this is not documented yet. We might have to
  ;; come up with a separate config for unresolved-namespace, but it's not yet
  ;; clear what it should look like.
  (when-not (config/unresolved-symbol-excluded config
                                               callstack symbol)
    (swap! namespaces update-in [base-lang lang ns-sym :unresolved-namespaces]
           conj (vary-meta unresolved-ns
                           ;; since the user namespaces is present in each file
                           ;; we must include the filename here
                           ;; see #73
                           assoc :filename filename))))

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
  ;; (prn "NAME" name-sym)
  (let [lang (:lang ctx)
        ns (get-namespace ctx (:base-lang ctx) lang ns-name)
        cljs? (identical? :cljs lang)]
    (if-let [ns* (namespace name-sym)]
      (let [ns* (if cljs? (str/replace ns* #"\$macros$" "")
                    ns*)
            ns-sym (symbol ns*)]
        (or (when-let [ns* (or (get (:qualify-ns ns) ns-sym)
                               ;; referring to the namespace we're in
                               (when (= (:name ns) ns-sym)
                                 ns-sym))]

              {:ns ns*
               :name (symbol (name name-sym))})
            (when-let [[class-name package]
                       (or (when (identical? :clj lang)
                             (or (find var-info/default-import->qname ns-sym)
                                 (when-let [v (get var-info/default-fq-imports ns-sym)]
                                   [v v])))
                           (find (:imports ns) ns-sym))]
              (reg-used-import! ctx ns-name class-name)
              {:interop? true
               :ns package
               :name (symbol (name name-sym))})
            (when-not (if (identical? :clj lang)
                        (or (one-of ns* ["clojure.core"])
                            (class-name? ns*))
                        (when cljs?
                          ;; see https://github.com/clojure/clojurescript/blob/6ed949278ba61dceeafb709583415578b6f7649b/src/main/clojure/cljs/analyzer.cljc#L781
                          (one-of ns* ["js" "goog" "cljs.core"
                                       "Math" "String" "goog.object" "goog.string"
                                       "goog.array"])))
              {:name (symbol (name name-sym))
               :unresolved? true
               :unresolved-ns ns-sym})))
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
                      (if cljs?
                        ;; CLJS allows imported classes to be used like this: UtcDateTime.fromTimestamp
                        ;; hmm, this causes the extractor to fuck up
                        (let [fs (first-segment name-sym)]
                          (find (:imports ns) fs))
                        (find (:imports ns) name-sym)))]
         ;; (prn "name-sym" name-sym*)
         (reg-used-import! ctx ns-name name-sym*)
         {:ns package
          :interop? true
          :name name-sym*})
       (when cljs?
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
