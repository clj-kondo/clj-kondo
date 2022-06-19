(ns clj-kondo.impl.namespace
  {:no-doc true}
  (:refer-clojure :exclude [ns-name])
  (:require
   [clj-kondo.impl.analysis :as analysis]
   [clj-kondo.impl.analysis.java :as java]
   [clj-kondo.impl.config :as config]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.utils :as utils
    :refer [export-ns-sym node->line deep-merge linter-disabled? one-of]]
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
                  (-> (node->line (:filename ctx)
                                  ns
                                  :duplicate-require
                                  (str "duplicate require of " ns))
                      (assoc :duplicate-ns (export-ns-sym ns))))
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
                prefix (:prefix m)
                raw-ns (cond prefix
                             (str prefix "." ns)
                             raw-ns (if (string? raw-ns)
                                      (pr-str raw-ns)
                                      (str raw-ns))
                             :else (str ns))
                branch (:branch m)
                raw-ns (str/lower-case raw-ns)]
            (cond branch
                  (recur last-processed-ns (next ns-list))
                  (pos? (compare last-processed-ns raw-ns))
                  (findings/reg-finding!
                   ctx
                   (node->line (:filename ctx)
                               ns
                               :unsorted-required-namespaces
                               (str "Unsorted namespace: " ns)))
                  :else (recur raw-ns
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
                         :name-row (or (:name-row metadata) (:row metadata))
                         :name-col (or (:name-col metadata) (:col metadata))
                         :name-end-row (or (:name-end-row metadata) (:end-row metadata))
                         :name-end-col (or (:name-end-col metadata) (:end-col metadata))
                         :row expr-row
                         :col expr-col
                         :end-row expr-end-row
                         :end-col expr-end-col)
         path [base-lang lang ns-sym]
         temp? (:temp metadata)
         config (:config ctx)]
     (when (and (:analysis ctx)
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
                (when (and top-level?
                           (not (:declared metadata))
                           (not (:in-comment ctx)))
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
                                 expr
                                 :redefined-var
                                 (if (= ns-sym redefined-ns)
                                   (str "redefined var #'" redefined-ns "/" var-sym)
                                   (str var-sym " already refers to #'" redefined-ns "/" var-sym)))))
                  (when-not temp?
                    (when (and (not (identical? :off (-> config :linters :missing-docstring :level)))
                               (not (:private metadata))
                               (not (:doc metadata))
                               (not (:test metadata))
                               (not temp?)
                               (not (:imported-var metadata))
                               (not
                                (when-let [defined-by (or (:linted-as metadata)
                                                          (:defined-by metadata))]
                                  (or
                                   (= 'clojure.test/deftest defined-by)
                                   (= 'clojure.core/deftype defined-by)
                                   (= 'clojure.core/defrecord defined-by)
                                   (= 'clojure.core/defprotocol defined-by)
                                   (= 'clojure.core/definterface defined-by)))))
                      (findings/reg-finding!
                       ctx
                       (node->line filename
                                   expr
                                   :missing-docstring
                                   "Missing docstring.")))
                    (when (and (identical? :clj lang)
                               (= '-main var-sym))
                      ;; TODO: and lang = :clj
                      (when-not (:gen-class ns)
                        (when-not (identical? :off (-> config :linters :main-without-gen-class :level))
                          (findings/reg-finding!
                           ctx
                           (node->line filename
                                       expr
                                       :main-without-gen-class
                                       "Main function without gen-class.")))))))
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
             (update ns :used-vars (fnil conj []) usage)))))

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
  (swap! namespaces
         (fn [n]
           (-> n
               (assoc-in [base-lang lang ns-sym :qualify-ns alias-sym] aliased-ns-sym)
               (assoc-in [base-lang lang ns-sym :aliases alias-sym] aliased-ns-sym)))))

(defn reg-binding!
  [ctx ns-sym binding]
  (when-not (or (:skip-reg-binding? ctx)
                (:clj-kondo/skip-reg-binding binding))
    (when (:analyze-locals? ctx)
      (analysis/reg-local! ctx (:filename ctx) binding))
    (when-not (or (:clj-kondo/mark-used binding)
                  (:mark-bindings-used? ctx))
      (let [{:keys [:base-lang :lang :namespaces]} ctx]
        (swap! namespaces update-in [base-lang lang ns-sym :bindings]
               conj binding))))
  nil)

(defn reg-destructuring-default!
  [{:keys [:base-lang :lang :namespaces :ns]} default binding]
  (swap! namespaces
         update-in [base-lang lang (:name ns) :destructuring-defaults]
         conj (assoc default :binding binding))
  nil)

(defn reg-used-binding!
  [{:keys [:base-lang :lang :namespaces :filename] :as ctx} ns-sym binding usage]
  (when (and usage (:analyze-locals? ctx) (not (:clj-kondo/mark-used binding)))
    (analysis/reg-local-usage! ctx filename binding usage))
  (swap! namespaces update-in [base-lang lang ns-sym :used-bindings]
         conj binding)
  nil)

(defn reg-required-namespaces!
  [{:keys [:base-lang :lang :namespaces] :as ctx} ns-sym analyzed-require-clauses]
  (lint-conflicting-aliases! ctx (:required analyzed-require-clauses))
  (lint-unsorted-required-namespaces! ctx (:required analyzed-require-clauses))
  (let [path [base-lang lang ns-sym]
        ns (get-in @namespaces path)]
    (lint-duplicate-requires! ctx (:required ns) (:required analyzed-require-clauses))
    (swap! namespaces update-in path
           (fn [ns]
             (merge-with into ns analyzed-require-clauses))))
  nil)

(defn reg-imports!
  [{:keys [:base-lang :lang :namespaces] :as ctx} ns-sym imports]
  (swap! namespaces update-in [base-lang lang ns-sym]
         (fn [ns]
           ;; TODO:
           ;; (lint-duplicate-imports! ctx (:required ns) ...)
           (update ns :imports merge imports)))
  (when (java/analyze-class-usages? ctx)
    (doseq [[k v] imports]
      (java/reg-class-usage! ctx (str v "." k) (assoc (meta k) :import true)))))

(defn class-name? [s]
  (let [^String s (str s)]
    (when-let [i (str/last-index-of s \.)]
      (or (let [should-be-capital-letter-idx (inc i)]
            (and (> (.length s) should-be-capital-letter-idx)
                 (Character/isUpperCase ^char (.charAt s should-be-capital-letter-idx))))
          (when-let [i (str/index-of s "_")]
            (pos? i))))))

(defn reg-unresolved-symbol!
  [ctx ns-sym sym {:keys [:base-lang :lang :config
                          :callstack] :as sym-info}]
  (when-not (or (:unresolved-symbol-disabled? sym-info)
                (config/unresolved-symbol-excluded config
                                                   callstack sym)
                (let [symbol-name (name sym)]
                  (or (and
                       (> (count symbol-name) 1)
                       (str/starts-with? symbol-name "."))
                      (class-name? symbol-name))))
    (swap! (:namespaces ctx) update-in [base-lang lang ns-sym :unresolved-symbols sym]
           (fnil conj [])
           sym-info))
  nil)

(defn reg-unresolved-var!
  [ctx ns-sym resolved-ns sym {:keys [:base-lang :lang :config] :as sym-info}]
  (when-not (or
             ;; this is set because of linting macro bodies
             ;; before removing this, check script/diff
             (:unresolved-symbol-disabled? sym-info)
             (config/unresolved-var-excluded config resolved-ns sym)
             (let [symbol-name (name sym)]
               (or (str/starts-with? symbol-name ".")
                   (class-name? symbol-name))))
    (swap! (:namespaces ctx) update-in
           [base-lang lang ns-sym :unresolved-vars [resolved-ns sym]]
           (fnil conj [])
           sym-info))
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
  [{:keys [:base-lang :lang :namespaces] :as ctx}
   name-sym ns-sym package class-name expr]
  (swap! namespaces update-in [base-lang lang ns-sym :used-imports]
         conj class-name)
  (when (java/analyze-class-usages? ctx)
    (let [name-meta (meta name-sym)
          loc (or (meta expr)
                  (meta class-name))]
      (java/reg-class-usage! ctx
                                  (str package "." class-name)
                                  (assoc loc
                                         :name-row (or (:row name-meta) (:row loc))
                                         :name-col (or (:col name-meta) (:col loc))
                                         :name-end-row (or (:end-row name-meta) (:end-row loc))
                                         :name-end-col (or (:end-col name-meta) (:end-col loc)))))))

(defn reg-unresolved-namespace!
  [{:keys [:base-lang :lang :namespaces :config :callstack :filename] :as _ctx} ns-sym unresolved-ns]
  (when-not
   (or
    (identical? :off (-> config :linters :unresolved-namespace :level))
    (config/unresolved-namespace-excluded config unresolved-ns)
       ;; unresolved namespaces in an excluded unresolved symbols call are not reported
    (config/unresolved-symbol-excluded config callstack :dummy))
    (let [unresolved-ns (vary-meta unresolved-ns
                                   ;; since the user namespaces is present in each filesrc/clj_kondo/impl/namespace.clj
                                   ;; we must include the filename here
                                   ;; see #73
                                   assoc :filename filename)]
      (swap! namespaces update-in [base-lang lang ns-sym :unresolved-namespaces unresolved-ns]
             (fnil conj [])
             unresolved-ns))))

(defn get-namespace [ctx base-lang lang ns-sym]
  (get-in @(:namespaces ctx) [base-lang lang ns-sym]))

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

(defn check-shadowed-binding! [ctx name-sym expr]
  (let [config (:config ctx)
        level (-> config :linters :shadowed-var :level)]
    (when-not (identical? :off level)
      (when-let [{:keys [:ns :name]}
                 (let [ns-name (:name (:ns ctx))
                       lang (:lang ctx)
                       ns (get-namespace ctx (:base-lang ctx) lang ns-name)]
                   (if-let [v (get (:referred-vars ns)
                                   name-sym)]
                     v
                     (if (contains? (:vars ns) name-sym)
                       {:ns (:name ns)
                        :name name-sym}
                       (let [clojure-excluded? (contains? (:clojure-excluded ns)
                                                          name-sym)
                             core-sym? (when-not clojure-excluded?
                                         (var-info/core-sym? lang name-sym))
                             special-form? (or (special-symbol? name-sym)
                                               (contains? var-info/special-forms name-sym))]
                         (when (or core-sym? special-form?)
                           {:ns (case lang
                                  :clj 'clojure.core
                                  :cljs 'cljs.core)
                            :name name-sym})))))]
        (when-not (config/shadowed-var-excluded? config name)
          (let [suggestions (get-in ctx [:config :linters :shadowed-var :suggest])
                suggestion (when suggestions
                             (get suggestions name))
                message (str "Shadowed var: " ns "/" name)
                message (if suggestion
                          (str message ". Suggestion: " suggestion)
                          message)]
            (findings/reg-finding! ctx (node->line (:filename ctx)
                                                   expr
                                                   :shadowed-var
                                                   message))))))))

(defn split-on-dots [name-sym]
  (vec (.split ^String (str name-sym) "\\.")))

(defn normalize-sym-name
  "Assumes simple symbol.
  Strips foo.bar.baz into foo if foo is a local or var, as it ignores property access in CLJS.
  When foo.bar is a known namespace, returns foo.bar/baz."
  [ctx sym]
  (let [lang (:lang ctx)]
    (if (identical? :cljs lang)
      (let [name-str (str sym)]
        (if (and
             (not (str/starts-with? name-str "."))
             (not (str/ends-with? name-str "."))
             (str/includes? name-str "."))
          (let [locals (:bindings ctx)
                sym-str (str sym)
                segments (split-on-dots sym-str)
                prefix-str (segments 0)
                prefix-sym (symbol prefix-str)
                the-ns (get-namespace ctx (:base-lang ctx) lang (-> ctx :ns :name))]
            (if (or (contains? locals prefix-sym)
                    (contains? (:vars the-ns) prefix-sym))
              prefix-sym
              (let [qualify-ns (:qualify-ns the-ns)
                    ns-resolved? (contains? qualify-ns sym)]
                (if ns-resolved? sym
                    (let [maybe-ns-str (str/join "." (subvec segments
                                                             0 (dec (count segments))))
                          maybe-ns (symbol maybe-ns-str)
                          ns-prefix? (= maybe-ns (get qualify-ns maybe-ns))]
                      (if ns-prefix?
                        ;; return fully qualified symbol
                        (symbol maybe-ns-str (peek segments))
                        (if-not (= "goog" prefix-str)
                          prefix-sym
                          sym)))))))
          sym))
      sym)))

(defn resolve-name
  [ctx call? ns-name name-sym expr]
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
              (let [core? (or (= 'clojure.core ns*)
                              (= 'cljs.core ns*))
                    var-name (symbol
                              ;; account for interop
                              (str/replace (str (name name-sym))
                                           #"\.$" ""))]
                (cond->
                    {:ns ns*
                     :name var-name}

                  (contains? (:aliases ns) ns-sym)
                  (assoc :alias ns-sym)

                  core?
                  (assoc :resolved-core? (var-info/core-sym? lang var-name)))))
            (when-let [[class-name package]
                       (or (when (identical? :clj lang)
                             (or (when-let [[class fq] (find var-info/default-import->qname ns-sym)]
                                   ;; TODO: fix in default-import->qname
                                   [class (str/replace fq (str/re-quote-replacement (str "." class)) "")])
                                 (when-let [fq (get var-info/default-fq-imports ns-sym)]
                                   (let [fq (str fq)]
                                     [(last (str/split fq #"\."))
                                      (str/join "." (butlast (str/split fq #"\.")))]))))
                           (find (:imports ns) ns-sym))]
              (reg-used-import! ctx name-sym ns-name package class-name expr)
              (when call? (findings/warn-reflection ctx expr))
              {:interop? true
               :ns (symbol (str package "." class-name))
               :name (symbol (name name-sym))})
            (if (identical? :clj lang)
              (if (and (not (one-of ns* ["clojure.core"]))
                       (class-name? ns*))
                (do (java/reg-class-usage! ctx ns* (meta expr))
                    (when call? (findings/warn-reflection ctx expr))
                    {:interop? true})
                {:name (symbol (name name-sym))
                 :unresolved? true
                 :unresolved-ns ns-sym})
              (if cljs?
                ;; see https://github.com/clojure/clojurescript/blob/6ed949278ba61dceeafb709583415578b6f7649b/src/main/clojure/cljs/analyzer.cljc#L781
                (when-not (one-of ns* ["js" "goog" "cljs.core"
                                       "Math" "String"])
                  {:name (symbol (name name-sym))
                   :unresolved? true
                   :unresolved-ns ns-sym})
                {:name (symbol (name name-sym))
                 :unresolved? true
                 :unresolved-ns ns-sym}))))
      (let [name-sym (if cljs?
                       ;; although we also check for CLJS in normalize-sym, we
                       ;; already know cljs? here
                       (normalize-sym-name ctx name-sym)
                       name-sym)]
        (if (and cljs? (namespace name-sym))
          (recur ctx call? ns-name name-sym expr)
          (or
           (when-let [[k v] (find (:referred-vars ns)
                                  name-sym)]
             (reg-used-referred-var! ctx ns-name k)
             v)
           (when (contains? (:vars ns) name-sym)
             {:ns (:name ns)
              :name name-sym})
           (when-let [[name-sym* package]
                      (or (when-let [[class fq] (find var-info/default-import->qname name-sym)]
                            ;; TODO: fix in default-import->qname
                            [class (str/replace fq (str/re-quote-replacement (str "." class)) "")])
                          ;; (find var-info/default-import->qname name-sym)
                          (when-let [v (get var-info/default-fq-imports name-sym)]
                            [v v])
                          (if cljs?
                            ;; CLJS allows imported classes to be used like this: UtcDateTime.fromTimestamp
                            (let [fs (first-segment name-sym)]
                              (find (:imports ns) fs))
                            (find (:imports ns) name-sym)))]
             (reg-used-import! ctx name-sym ns-name package name-sym* expr)
             (when call? (findings/warn-reflection ctx expr))
             {:ns package
              :interop? true
              :name name-sym*})
           (when cljs?
             (when-let [ns* (get (:qualify-ns ns) name-sym)]
               (when (some-> (meta ns*) :raw-name string?)
                 {:ns ns*
                  :name name-sym})))
           (let [clojure-excluded? (contains? (:clojure-excluded ns)
                                              name-sym)]
             (if (or
                  ;; check core-sym
                  (when-not clojure-excluded?
                    (var-info/core-sym? lang name-sym))
                  ;; check special form
                  (and call?
                       (or (special-symbol? name-sym)
                           (contains? var-info/special-forms name-sym))))
               {:ns (case lang
                      :clj 'clojure.core
                      :cljs 'cljs.core)
                :name name-sym
                :resolved-core? true}
               (let [referred-all-ns (some (fn [[k {:keys [:excluded]}]]
                                             (when-not (contains? excluded name-sym)
                                               k))
                                           (:refer-alls ns))]
                 (if (and (not referred-all-ns)
                          (class-name? name-sym))
                   (do (java/reg-class-usage! ctx (str name-sym) (meta expr))
                       (when call? (findings/warn-reflection ctx expr))
                       {:interop? true})
                   {:ns (or referred-all-ns :clj-kondo/unknown-namespace)
                    :name name-sym
                    :unresolved? true
                    :clojure-excluded? clojure-excluded?}))))))))))

;;;; Scratch

(comment
  )
