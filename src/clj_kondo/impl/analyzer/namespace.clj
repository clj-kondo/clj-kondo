(ns clj-kondo.impl.analyzer.namespace
  {:no-doc true}
  (:refer-clojure :exclude [ns-name])
  (:require
   [clj-kondo.impl.analysis :as analysis]
   [clj-kondo.impl.analysis.java :as java]
   [clj-kondo.impl.analyzer.common :as common]
   [clj-kondo.impl.analyzer.usages :as usages]
   [clj-kondo.impl.cache :as cache]
   [clj-kondo.impl.config :as config]
   [clj-kondo.impl.docstring :as docstring]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.metadata :as meta]
   [clj-kondo.impl.namespace :as namespace]
   [clj-kondo.impl.utils :as utils
    :refer [assoc-some linter-disabled? node->line one-of sexpr
            string-from-token tag token-node vector-node]]
   [clj-kondo.impl.var-info :refer [core-sym?]]
   [clojure.set :as set]
   [clojure.string :as str]))

(set! *warn-on-reflection* true)
(def valid-ns-name? (some-fn symbol? string?))

(def empty-spec? (every-pred sequential? empty?))

(defn- prefix-spec?
  "Adapted from clojure.tools.namespace"
  [form]
  (and (sequential? form) ; should be a list, but often is not
       (symbol? (first form))
       (not-any? keyword? form)
       (< 1 (count form)))) ; not a bare vector like [foo]

(defn- option-spec?
  "Adapted from clojure.tools.namespace"
  [form]
  (and (sequential? form) ; should be a vector, but often is not
       (valid-ns-name? (first form))
       (or (keyword? (second form)) ; vector like [foo :as f]
           (= 1 (count form))))) ; bare vector like [foo]

(defn normalize-libspec
  "Adapted from clojure.tools.namespace."
  [ctx prefix libspec-expr unused-namespace-disabled?]
  (let [libspec-expr (meta/lift-meta-content2 ctx libspec-expr)
        children (:children libspec-expr)
        form (sexpr libspec-expr)
        find-fn! #(findings/reg-finding! ctx (node->line (:filename ctx) libspec-expr :syntax %))]
    (cond (prefix-spec? form)
          (do (when prefix (find-fn! "Prefix lists can only have two levels."))
              (mapcat (fn [f]
                        (normalize-libspec ctx
                                           (symbol (str (when prefix (str prefix "."))
                                                        (first form)))
                                           f
                                           unused-namespace-disabled?))
                      (rest children)))
          (option-spec? form)
          [(with-meta
             (vector-node (into (normalize-libspec ctx prefix (first children) unused-namespace-disabled?)
                                (rest children)))
             (meta libspec-expr))]
          (valid-ns-name? form)
          (let [full-form (symbol (str (when prefix (str prefix "."))
                                       form))]
            (when (and prefix
                       (str/includes? (str form) "."))
              (find-fn!
               (format "found lib name '%s' containing period with prefix '%s'. lib names inside prefix lists must not contain periods."
                       form prefix)))
            [(with-meta (token-node full-form)
               (cond-> (assoc (meta libspec-expr)
                              :raw-name form
                              :unused-namespace-disabled unused-namespace-disabled?)
                 prefix
                 (assoc :prefix prefix)))])
          (keyword? form) ; Some people write (:require ... :reload-all)
          nil
          (empty-spec? form)
          (do (find-fn! "require form is invalid: clauses must not be empty") nil)
          :else
          (do
            (find-fn! (format "Unparsable libspec: %s" form))
            nil))))

(defn lint-alias-consistency [ctx ns-name alias]
  (let [consistent-aliases (get-in ctx [:config :linters :consistent-alias :aliases])]
    (when-let [expected-alias (or (get consistent-aliases ns-name)
                                  (get consistent-aliases (str ns-name)))]
      (when-not (= expected-alias alias)
        (findings/reg-finding!
         ctx
         (node->line (:filename ctx) alias
                     :consistent-alias
                     (str "Inconsistent alias. Expected " expected-alias " instead of " alias ".")))))))

(defn- lint-duplicates!
  [ctx nodes linter-type message-prefix]
  (when-not (linter-disabled? ctx linter-type)
    (reduce (fn [seen node]
              (if (utils/ignored? node)
                seen
                (let [v (:value node)]
                  (if (contains? seen v)
                    (do
                      (findings/reg-finding!
                       ctx
                       (node->line (:filename ctx)
                                   node
                                   linter-type
                                   (str message-prefix v)))
                      seen)
                    (conj seen v)))))
            #{} nodes)))

(defn- lint-duplicate-refers! [ctx refers]
  (lint-duplicates! ctx refers :duplicate-refer "Duplicate refer: "))

(defn- lint-duplicate-excludes! [ctx excludes]
  (lint-duplicates! ctx excludes :duplicate-exclude "Duplicate exclude: "))

(defn analyze-libspec
  [ctx current-ns-name require-kw-expr libspec-expr]
  (utils/handle-ignore ctx libspec-expr)
  (let [lang (:lang ctx)
        base-lang (:base-lang ctx)
        filename (:filename ctx)
        require-sym (:value require-kw-expr)
        require-kw (or (:k require-kw-expr)
                       (when require-sym
                         (keyword require-sym)))
        use? (= :use require-kw)
        libspec-meta (meta libspec-expr)
        config (:config ctx)
        linters (:linters config)
        lint-refers? (not (identical? :off (-> linters :refer :level)))
        unknown-require-option-config (-> linters :unknown-require-option)
        req-macros? (= :require-macros require-kw)]
    (if-let [s (utils/symbol-from-token libspec-expr)]
      (do
        (when (and (= s current-ns-name)
                   (not req-macros?)
                   (not (:in-comment ctx)))
          (findings/reg-finding!
           ctx
           (node->line
            filename
            libspec-expr
            :self-requiring-namespace
            (str "Namespace is requiring itself: "
                 s))))
        [{:type :require
          :referred-all (when use? require-kw-expr)
          :ns (with-meta s
                (assoc libspec-meta
                       :filename filename))}])
      (let [[ns-name-expr & option-exprs] (:children libspec-expr)
            ns-name (:value ns-name-expr)
            ns-name (if (= :cljs lang)
                      (case ns-name
                        clojure.test 'cljs.test
                        clojure.pprint 'cljs.pprint
                        ns-name)
                      ns-name)
            ns-name (with-meta ns-name
                      (assoc (meta (first (:children libspec-expr)))
                             :filename filename
                             :raw-name (-> (meta ns-name-expr) :raw-name)
                             :branch (:branch libspec-meta)))
            cljs-macros-self-require? (and
                                       (= :cljc base-lang)
                                       (= :cljs lang)
                                       (= current-ns-name ns-name)
                                       req-macros?)]
        (when (and (= ns-name current-ns-name)
                   (not req-macros?)
                   (not (:in-comment ctx))
                   (not (= :as-alias (:k (first option-exprs)))))
          (findings/reg-finding!
           ctx
           (node->line
            filename
            libspec-expr
            :self-requiring-namespace
            (str "Namespace is requiring itself: " current-ns-name))))
        (loop [children option-exprs
               m {:as nil
                  :referred #{}
                  :excluded #{}
                  :referred-all (when use? require-kw-expr)
                  :renamed {}}]
          (if-let [child-expr (first children)]
            (let [opt-expr (fnext children)
                  opt (when opt-expr (sexpr opt-expr))
                  child-k (:k child-expr)]
              (utils/handle-ignore ctx opt-expr)
              (case child-k
                (:refer :refer-macros :only)
                (do
                  (when (and lint-refers? (not use?))
                    (when-not (config/refer-excluded? config ns-name)
                      (findings/reg-finding!
                       ctx
                       (node->line (:filename ctx)
                                   child-expr
                                   :refer
                                   (str "require with " child-k)))))
                  (recur
                   (nnext children)
                   (cond (sequential? opt)
                         (let [;; undo referred-all when using :only with :use
                               m (if (and use? (= :only child-k))
                                   (do (findings/reg-finding!
                                        ctx
                                        (node->line
                                         filename
                                         (:referred-all m)
                                         :use
                                         (format "use %srequire with alias or :refer [%s]"
                                                 (if require-sym
                                                   "" ":")
                                                 (str/join " " (sort opt)))))
                                       (dissoc m :referred-all))
                                   m)
                               opt-expr-children (:children opt-expr)]
                           (run! #(utils/handle-ignore ctx %) opt-expr-children)
                           (lint-duplicate-refers! ctx opt-expr-children)
                           (when (:analyze-var-usages? ctx)
                             (run! #(namespace/reg-var-usage! ctx current-ns-name
                                                              (let [m (meta %)]
                                                                (assoc m
                                                                       :type :use
                                                                       :name (with-meta (sexpr %) m)
                                                                       :resolved-ns ns-name
                                                                       :ns current-ns-name
                                                                       :refer true
                                                                       :lang lang
                                                                       :base-lang base-lang
                                                                       :filename filename
                                                                       :config config
                                                                       :expr %)))
                                   opt-expr-children))
                           (swap! (:used-namespaces ctx) update (:base-lang ctx) conj ns-name)
                           (update m :referred into
                                   (map #(with-meta (sexpr %)
                                           (cond-> (meta %)
                                             cljs-macros-self-require?
                                             (assoc :cljs-macro-self-require true)))) opt-expr-children))
                         (= :all opt)
                         (assoc m :referred-all opt-expr)
                         :else m)))
                (:as :as-alias)
                (recur
                 (nnext children)
                 (assoc m
                        :as (when opt
                              (with-meta opt
                                (cond-> (assoc (meta opt-expr)
                                               :filename (:filename ctx))
                                  (identical? :as-alias child-k)
                                  (assoc :as-alias true))))))
                ;; shadow-cljs:
                ;; https://shadow-cljs.github.io/docs/UsersGuide.html#_about_default_exports
                :default
                (recur (nnext children)
                       (update m :referred conj (with-meta opt
                                                  (meta opt-expr))))
                :exclude
                (do
                  (lint-duplicate-excludes! ctx (:children opt-expr))
                  (recur
                   (nnext children)
                   (update m :excluded into (set opt))))
                :rename
                (let [opt (zipmap (keys opt) (map #(with-meta (sexpr %) (meta %))
                                                  (take-nth 2 (rest (:children opt-expr)))))]
                  (recur
                   (nnext children)
                   (-> m (update :renamed merge opt)
                       ;; for :refer-all we need to know the excluded
                       (update :excluded into (set (keys opt)))
                       ;; for :refer it is sufficient to pretend they were never referred
                       (update :referred set/difference (set (keys opt))))))
                :include-macros
                (do
                  (if (#{:cljc :cljs} base-lang)
                    (when-not (true? opt)
                      (findings/reg-finding!
                       ctx
                       (node->line
                        filename
                        child-expr
                        :syntax
                        "Require form is invalid: :invalid-macros only accepts true")))
                    (when-not (contains? (set (:exclude unknown-require-option-config)) child-k)
                      (findings/reg-finding!
                       ctx
                       (node->line
                        filename
                        child-expr
                        :unknown-require-option
                        (format "Unknown require option: %s"
                                child-k)))))
                  (recur (nnext children)
                         m))
                (do (when-not (contains? (set (:exclude unknown-require-option-config)) child-k)
                      (findings/reg-finding!
                       ctx
                       (node->line
                        filename
                        child-expr
                        :unknown-require-option
                        (format "Unknown require option: %s"
                                child-k))))
                    (recur (nnext children)
                           m))))
            (let [{:keys [as referred excluded referred-all renamed]} m
                  referred (if (and referred-all
                                    (identical? :clj base-lang))
                             (let [referred (cache/with-thread-lock
                                              (cache/with-cache (:cache-dir ctx) 6
                                                (cache/from-cache-1 (:cache-dir ctx) :clj ns-name)))]
                               (keep (fn [[k v]]
                                       (when-not (:class v)
                                         k))
                                     referred))
                             referred)]
              (when as (lint-alias-consistency ctx ns-name as))
              [{:type :require
                :ns (vary-meta ns-name assoc :alias as)
                :as as
                :require-kw require-kw
                :excluded excluded
                :referred (concat (map (fn [r]
                                         [r (cond-> {:ns ns-name
                                                     :name r
                                                     :filename filename
                                                     :config config}
                                              (:cljs-macro-self-require (meta r))
                                              (assoc :cljs-macro-self-require true))])
                                       referred)
                                  (map (fn [[original-name new-name]]
                                         [new-name {:ns ns-name
                                                    :name original-name
                                                    :filename filename
                                                    :config config}])
                                       renamed))
                :referred-all referred-all}])))))))

(defn coerce-class-symbol [ctx node]
  (if-let [v (:value node)]
    (with-meta v
      (meta node))
    (do (findings/reg-finding!
         ctx
         (node->line (:filename ctx) node :syntax "Expected: class symbol"))
        nil)))

(defn analyze-import [ctx _ns-name libspec-expr]
  (utils/handle-ignore ctx libspec-expr)
  (case (tag libspec-expr)
    (:vector :list) (let [children (:children libspec-expr)
                          java-package-name-node (first children)
                          java-package (:value java-package-name-node)
                          imported-nodes (rest children)
                          imported (keep #(coerce-class-symbol ctx %) imported-nodes)]
                      (run! #(utils/handle-ignore ctx %) imported-nodes)
                      (cond (empty? children)
                            (findings/reg-finding!
                             ctx
                             (node->line
                              (:filename ctx) libspec-expr
                              :syntax "import form is invalid: clauses must not be empty"))
                            (empty? imported-nodes)
                            (findings/reg-finding!
                             ctx
                             (node->line
                              (:filename ctx) java-package-name-node
                              :syntax "Expected: package name followed by classes.")))
                      (into {} (for [i imported]
                                 [i java-package])))
    :token (if (symbol? (:value libspec-expr))
             (let [package+class (:value libspec-expr)
                   splitted (-> package+class name (str/split #"\."))
                   java-package (symbol (str/join "." (butlast splitted)))
                   imported (with-meta (symbol (last splitted))
                              (meta libspec-expr))]
               {imported java-package})
             (do
               (findings/reg-finding!
                ctx
                (node->line
                 (:filename ctx)
                 libspec-expr
                 :syntax "Import target is not a package"))
               {}))
    nil))

(defn analyze-require-clauses [ctx ns-name kw+libspecs]
  (let [lang (:lang ctx)
        unused-namespace-disabled? (identical? :off (-> ctx :config :linters :unused-namespace :level))
        analyzed
        (map (fn [[require-kw libspecs]]
               (when-not libspecs
                 (findings/reg-finding!
                  ctx (node->line (:filename ctx) require-kw :syntax
                                  "Invalid require: no libs specified to load")))
               (for [libspec-expr libspecs
                     normalized-libspec-expr (normalize-libspec ctx nil libspec-expr unused-namespace-disabled?)
                     analyzed (analyze-libspec ctx ns-name require-kw normalized-libspec-expr)]
                 analyzed))
             kw+libspecs)
        _ (doseq [analyzed analyzed]
            (namespace/lint-conflicting-aliases! ctx analyzed)
            (let [namespaces (map :ns analyzed)]
              (namespace/lint-unsorted-required-namespaces! ctx namespaces)
              (namespace/lint-duplicate-requires! ctx namespaces)))
        analyzed (apply concat analyzed)
        refer-alls (reduce (fn [acc clause]
                             (if-let [m (:referred-all clause)]
                               (assoc acc (:ns clause)
                                      {:excluded (:excluded clause)
                                       :node m
                                       :referred #{}
                                       :filename (:filename ctx)})
                               acc))
                           {}
                           analyzed)
        required-namespaces (map :ns analyzed)]
    {:required required-namespaces
     :qualify-ns (reduce (fn [acc sc]
                           (let [n (:ns sc)
                                 as (:as sc)
                                 new? (not (contains? acc n))
                                 ;; if alias foo exists and there is a
                                 ;; namespaces fully written as foo, the alias
                                 ;; wins, see #864
                                 acc (if new? (assoc acc n n) acc)
                                 ;; For the same reason, if there is an alias,
                                 ;; assoc it regardless of whether there was
                                 ;; already a namespace name here
                                 acc (if as (assoc acc as n) acc)]
                             acc))
                         {ns-name ns-name}
                         analyzed)
     :aliases (into {} (comp (filter :as) (map (juxt :as :ns))) analyzed)
     :ns->aliases (when-not (-> ctx :config :linters :aliased-namespace-symbol :level
                                (identical? :off))
                    (reduce
                     (fn [acc sc]
                       (let [n (:ns sc)
                             as (:as sc)
                             existing (or (acc n) #{})]
                         (if as
                           (assoc acc n (conj existing as))
                           acc)))
                     {}
                     analyzed))
     :referred-vars (into {} (mapcat :referred analyzed))
     :refer-alls refer-alls
     :used-namespaces
     (-> (case lang
           :clj '#{clojure.core}
           :cljs '#{cljs.core})
         (into (keys refer-alls))
         (conj ns-name)
         (into (when-not
                (-> ctx :config :linters :unused-namespace :simple-libspec)
                 (keep (fn [req]
                         (when (and (not (:as req))
                                    (empty? (:referred req)))
                           (:ns req)))
                       analyzed))))}))

(defn new-namespace [filename base-lang lang ns-name typ row col]
  {:type typ
   :filename filename
   :base-lang base-lang
   :lang lang
   :name ns-name
   :bindings #{}
   :used-bindings #{}
   :destructuring-defaults #{}
   :used-referred-vars #{}
   :used-imports #{}
   :used-aliases #{}
   :used-vars []
   :unresolved-namespaces {}
   :vars nil
   :row row
   :col col})

(defn- lint-refer-clojure-vars [{:keys [filename lang] :as ctx} excluded-vars]
  (letfn [(exists-in-core? [excluded-var lang]
            (if (= :cljc (:base-lang ctx))
              (some #(core-sym? % excluded-var) [:clj :cljs])
              (core-sym? lang excluded-var)))]
    (when-not (linter-disabled? ctx :unresolved-excluded-var)
      (doseq [excluded-var excluded-vars
              :when (not (or (exists-in-core? excluded-var lang)
                             (utils/ignored? excluded-var)))]
        (findings/reg-finding!
         ctx
         (node->line filename excluded-var
                     :unresolved-excluded-var
                     (str "Unresolved excluded var: "
                          excluded-var)))))))

(defn analyze-ns-decl
  [ctx expr]
  (when (:analyze-keywords? ctx)
    (usages/analyze-usages2 ctx expr {:quote? true}))
  (let [lang (:lang ctx)
        base-lang (:base-lang ctx)
        filename (:filename ctx)
        m (meta expr)
        row (:row m)
        col (:col m)
        children (next (:children expr))
        ns-name-expr (first children)
        ns-name-metas (:meta ns-name-expr)
        ns-name-expr (meta/lift-meta-content2 ctx ns-name-expr)
        metadata (meta ns-name-expr)
        children (next children) ;; first = docstring, attr-map or libspecs
        fc (first children)
        docstring (when fc
                    (string-from-token fc))
        doc-node-raw (when docstring
                       fc)
        meta-node (when fc
                    (let [t (tag fc)]
                      (if (= :map t)
                        fc
                        (when-let [sc (second children)]
                          (when (= :map (tag sc))
                            sc)))))
        _ (when meta-node (common/analyze-expression** ctx meta-node))
        meta-node-meta (when meta-node
                         (try (sexpr meta-node)
                              (catch Exception _ nil)))
        ns-meta (if meta-node-meta
                  (merge metadata meta-node-meta)
                  metadata)
        deprecated (:deprecated ns-meta)
        [doc-node docstring] (or (and meta-node-meta
                                      (:doc meta-node-meta)
                                      (docstring/docs-from-meta meta-node))
                                 [doc-node-raw docstring])
        [doc-node docstring] (if docstring
                               [doc-node docstring]
                               (when (some-> metadata :doc str)
                                 (some docstring/docs-from-meta ns-name-metas)))
        global-config (:global-config ctx)
        ns-name (or
                 (when-let [?name (when ns-name-expr (sexpr ns-name-expr))]
                   (if (symbol? ?name) ?name
                       (findings/reg-finding!
                        ctx
                        (node->line (:filename ctx)
                                    ns-name-expr
                                    :syntax
                                    "namespace name expected"))))
                 'user)
        _ (when-not (= 'user ns-name)
            (reset! (:main-ns ctx) ns-name))
        ns-groups (config/ns-groups ctx global-config ns-name filename)
        config-in-ns (let [config-in-ns (:config-in-ns global-config)]
                       (apply config/merge-config!
                              (concat (map #(get config-in-ns %) ns-groups)
                                      [(get config-in-ns ns-name)])))
        config-in-ns (config/expand-ignore config-in-ns)
        local-config (let [{:clj-kondo/keys [config ignore]} ns-meta]
                       (cond-> config
                         ignore (assoc :ignore ignore)))
        local-config (config/unquote local-config)
        local-config (config/expand-ignore local-config)
        merged-config (if config-in-ns
                        (config/merge-config! global-config config-in-ns)
                        global-config)
        merged-config (if local-config
                        (config/merge-config! merged-config local-config)
                        merged-config)
        ctx (if (or config-in-ns local-config)
              (assoc ctx :config merged-config)
              ctx)
        _ (when (and (not= "<stdin>" filename)
                     (not= 'user ns-name)
                     (not (identical? :off (-> ctx :config :linters :namespace-name-mismatch :level))))
            ;; users should be able to disable linter without hitting this code-path
            (let [filename* (some-> filename
                                    ^String (utils/strip-file-ext)
                                    ^String (.replace "/" ".")
                                    ;; Windows, but do unconditionally, see issue 1607
                                    (.replace "\\" "."))
                  munged-ns (str (namespace-munge ns-name))]
              (when (and filename*
                         (not (str/ends-with? filename* munged-ns)))
                (when-not (some (fn [m]
                                  (and (identical? :namespace-name-mismatch (:type m))
                                       (= filename (:filename m))))
                                @(:findings ctx))
                  (findings/reg-finding!
                   ctx
                   (node->line filename
                               ns-name-expr
                               :namespace-name-mismatch
                               (str "Namespace name does not match file name: " ns-name)))))))

        _ (when (and (not (identical? :off (-> ctx :config :linters :underscore-in-namespace :level)))
                     (symbol? ns-name)
                     (str/includes? (name ns-name) "_"))
            (findings/reg-finding!
             ctx
             (node->line filename
                         ns-name-expr
                         :underscore-in-namespace
                         (str "Avoid underscore in namespace name: " ns-name))))

        clauses (cond-> children
                  doc-node-raw next
                  meta-node next)
        _ (run! #(utils/handle-ignore ctx %) children)
        kw+libspecs (for [?require-clause clauses
                          :let [require-kw-node (-> ?require-clause :children first)
                                require-kw (:k require-kw-node)
                                require-kw (one-of require-kw [:require :require-macros :use :require-global])]
                          :when require-kw]
                      [require-kw-node (-> ?require-clause :children next)])
        analyzed-require-clauses
        (analyze-require-clauses ctx ns-name kw+libspecs)
        imports-raw (for [?import-clause clauses
                          :let [import-kw (= :import (some-> ?import-clause :children first :k))]
                          :when import-kw
                          libspec-expr (rest (:children ?import-clause))]
                      libspec-expr)
        _ (when (seq imports-raw)
            (namespace/lint-unsorted-required-namespaces! ctx imports-raw :unsorted-imports))
        imports
        (apply merge (map #(analyze-import ctx ns-name %) imports-raw))
        sexpr-clauses (map sexpr (nnext (:children expr)))
        refer-clojure-clauses
        (apply merge-with into
               (for [?refer-clojure-clause clauses
                     :let [?refer-clojure (sexpr ?refer-clojure-clause)]
                     :when (= :refer-clojure (first ?refer-clojure))
                     :let [options (-> ?refer-clojure-clause :children rest)
                           sexpr-options (rest ?refer-clojure)]
                     [[k v] [_ v-node]] (map vector
                                             (partition 2 sexpr-options)
                                             (partition 2 options))
                     :let [r (case k
                               :exclude
                               {:excluded (set v)
                                :exclude-nodes (:children v-node)}
                               :rename
                               {:renamed v
                                :excluded (set (keys v))}
                               :only
                               {:only (set v)})]]
                 r))
        _ (when-let [exclude-nodes (:exclude-nodes refer-clojure-clauses)]
            (lint-duplicate-excludes! ctx exclude-nodes))
        refer-clj {:referred-vars
                   (into {} (map (fn [[original-name new-name]]
                                   [new-name {:ns 'clojure.core
                                              :name original-name}])
                                 (:renamed refer-clojure-clauses)))
                   :clojure-excluded (:excluded refer-clojure-clauses)}
        refer-cljs-globals
        (when (identical? :cljs lang)
          (let [refer-globals (reduce merge {}
                                      (for [?refer-clojure sexpr-clauses
                                            :when (= :refer-global (first ?refer-clojure))
                                            :let [{:keys [only rename]} (apply hash-map (rest ?refer-clojure))]]
                                        (if rename
                                          (merge (set/map-invert rename) (let [onlies (remove rename only)]
                                                                           (zipmap onlies onlies)))
                                          (zipmap only only))))]
            {:referred-globals refer-globals}))
        _ (when (seq (:clojure-excluded refer-clj))
            (lint-refer-clojure-vars ctx (:clojure-excluded refer-clj)))
        gen-class? (some #(= :gen-class (some-> % :children first :k)) clauses)
        leftovers (for [clause clauses
                        :let [valid-kw (-> clause :children first :k)
                              valid-kw (one-of valid-kw [:require :require-macros :use
                                                         :import :refer-clojure
                                                         :load :gen-class
                                                         :require-global :refer-global])]
                        :when (not valid-kw)]
                    clause)
        _ (when (seq leftovers)
            (namespace/lint-unknown-clauses ctx leftovers))
        ns (cond->
            (merge (assoc (new-namespace filename base-lang lang ns-name :ns row col)
                          :imports imports
                          :gen-class gen-class?
                          :deprecated deprecated)
                   (merge-with into
                               analyzed-require-clauses
                               refer-clj
                               refer-cljs-globals))
             (or config-in-ns local-config) (assoc :config merged-config)
             (identical? :clj lang) (update :qualify-ns
                                            #(assoc % 'clojure.core 'clojure.core))
             (identical? :cljs lang) (update :qualify-ns
                                             #(assoc % 'cljs.core 'cljs.core
                                                     'clojure.core 'cljs.core)))]
    (when (:analysis ctx)
      (doseq [[k v] imports]
        (java/reg-class-usage! ctx (str v "." k) nil (assoc (meta k) :import true)))
      (analysis/reg-namespace! ctx filename row col
                               ns-name false (assoc-some {}
                                                         :user-meta (when (:analysis-ns-meta ctx)
                                                                      (conj (:user-meta metadata) meta-node-meta))
                                                         :end-row (:end-row m)
                                                         :end-col (:end-col m)
                                                         :name-row (:row metadata)
                                                         :name-col (:col metadata)
                                                         :name-end-row (:end-row metadata)
                                                         :name-end-col (:end-col metadata)
                                                         :deprecated (:deprecated ns-meta)
                                                         :doc docstring
                                                         :added (:added ns-meta)
                                                         :no-doc (:no-doc ns-meta)
                                                         :author (:author ns-meta)))
      (doseq [req (:required ns)]
        (let [{:keys [row col end-row end-col alias]} (meta req)
              meta-alias (meta alias)]
          (analysis/reg-namespace-usage! ctx filename row col ns-name
                                         req alias {:name-row row
                                                    :name-col col
                                                    :name-end-row end-row
                                                    :name-end-col end-col
                                                    :alias-row (:row meta-alias)
                                                    :alias-col (:col meta-alias)
                                                    :alias-end-row (:end-row meta-alias)
                                                    :alias-end-col (:end-col meta-alias)}))))
    (docstring/lint-docstring! ctx doc-node docstring)
    (namespace/reg-namespace! ctx ns)
    ns))

(defn analyze-require
  "For now we only support the form (require '[...])"
  [ctx expr]
  (let [ns-name (-> ctx :ns :name)
        [require-node & children] (:children expr)
        [libspecs non-quoted-children]
        (utils/keep-remove #(let [t (tag %)]
                              (or (when (= :quote t)
                                    (first (:children %)))
                                  (let [children (:children %)]
                                    (when (and (= :list t)
                                               (= 'quote (some-> children first
                                                                 utils/symbol-from-token)))
                                      (second children)))))
                           children)
        ctx (if (some #{:reload :reload-all} (map :k non-quoted-children))
              (utils/ctx-with-linter-disabled ctx :duplicate-require)
              ctx)]
    (when-not (seq children)
      (findings/reg-finding!
       ctx (node->line (:filename ctx) require-node :syntax
                       "Invalid require: no libs specified to load")))
    (when (some-> children first sexpr empty-spec?)
      (findings/reg-finding!
       ctx
       (node->line (:filename ctx)
                   (first children)
                   :syntax "require form is invalid: clauses must not be empty")))
    (when (:analyze-keywords? ctx)
      (run! #(usages/analyze-usages2 ctx % {:quote? true}) libspecs))
    (let [analyzed
          (analyze-require-clauses ctx ns-name [[require-node libspecs]])]
      (namespace/reg-required-namespaces! ctx ns-name analyzed)
      (when (:analysis ctx)
        (doseq [req (:required analyzed)]
          (let [{:keys [row col end-row end-col alias]} (meta req)
                meta-alias (meta alias)]
            (analysis/reg-namespace-usage! ctx (:filename ctx)
                                           row col ns-name
                                           req alias {:name-row row
                                                      :name-col col
                                                      :name-end-row end-row
                                                      :name-end-col end-col
                                                      :alias-row (:row meta-alias)
                                                      :alias-col (:col meta-alias)
                                                      :alias-end-row (:end-row meta-alias)
                                                      :alias-end-col (:end-col meta-alias)})))))
    ;; also analyze children that weren't quoted
    (common/analyze-children ctx non-quoted-children)))

;;;; Scratch

(comment)
