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
    :refer [node->line one-of tag sexpr vector-node
            token-node string-from-token symbol-from-token
            assoc-some]]
   [clojure.set :as set]
   [clojure.string :as str]))

(set! *warn-on-reflection* true)
(def valid-ns-name? (some-fn symbol? string?))

(defn- prefix-spec?
  "Adapted from clojure.tools.namespace"
  [form]
  (and (sequential? form) ; should be a list, but often is not
       (symbol? (first form))
       (not-any? keyword? form)
       (< 1 (count form))))  ; not a bare vector like [foo]

(defn- option-spec?
  "Adapted from clojure.tools.namespace"
  [form]
  (and (sequential? form)  ; should be a vector, but often is not
       (valid-ns-name? (first form))
       (or (keyword? (second form))  ; vector like [foo :as f]
           (= 1 (count form)))))  ; bare vector like [foo]

(defn normalize-libspec
  "Adapted from clojure.tools.namespace."
  [ctx prefix libspec-expr]
  (let [libspec-expr (meta/lift-meta-content2 ctx libspec-expr)
        children (:children libspec-expr)
        form (sexpr libspec-expr)]
    (cond (prefix-spec? form)
          (do (when prefix
                (findings/reg-finding! ctx (node->line (:filename ctx)
                                                       libspec-expr
                                                       :syntax
                                                       "Prefix lists can only have two levels.")))
              (mapcat (fn [f]
                        (normalize-libspec ctx
                                           (symbol (str (when prefix (str prefix "."))
                                                        (first form)))
                                           f))
                      (rest children)))
          (option-spec? form)
          [(with-meta
             (vector-node (into (normalize-libspec ctx prefix (first children)) (rest children)))
             (meta libspec-expr))]
          (valid-ns-name? form)
          (let [full-form (symbol (str (when prefix (str prefix "."))
                                       form))]
            (when (and prefix
                       (str/includes? (str form) "."))
              (findings/reg-finding! ctx
                                     (node->line (:filename ctx)
                                                 libspec-expr
                                                 :syntax
                                                 (format "found lib name '%s' containing period with prefix '%s'. lib names inside prefix lists must not contain periods."
                                                         form prefix))))
            [(with-meta (token-node full-form)
               (cond-> (assoc (meta libspec-expr)
                              :raw-name form)
                 prefix
                 (assoc :prefix prefix)))])
          (keyword? form)  ; Some people write (:require ... :reload-all)
          nil
          :else
          (throw (ex-info "Unparsable namespace form"
                          {:reason ::unparsable-ns-form
                           :form form})))))

(defn lint-alias-consistency [ctx ns-name alias]
  (let [config (:config ctx)]
    (when-let [expected-alias (get-in config [:linters :consistent-alias :aliases ns-name])]
      (when-not (= expected-alias alias)
        (findings/reg-finding!
         ctx
         (node->line (:filename ctx) alias
                     :consistent-alias
                     (str "Inconsistent alias. Expected " expected-alias " instead of " alias ".")))))))

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
        lint-refers? (not (identical? :off (-> config :linters :refer :level)))]
    (if-let [s (symbol-from-token libspec-expr)]
      [{:type :require
        :referred-all (when use? require-kw-expr)
        :ns (with-meta s
              (assoc libspec-meta
                     :filename filename))}]
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
            self-require? (and
                           (= :cljc base-lang)
                           (= :cljs lang)
                           (= current-ns-name ns-name)
                           (= :require-macros require-kw))]
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
                                   (str "require with " (str child-k))))))
                  (recur
                   (nnext children)
                   (cond (and (not self-require?) (sequential? opt))
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
                                           (meta %))) opt-expr-children))
                         (= :all opt)
                         (assoc m :referred-all opt-expr)
                         :else m)))
                (:as :as-alias)
                (recur
                 (nnext children)
                 (assoc m
                        :as (with-meta opt
                              (cond-> (meta opt-expr)
                                (identical? :as-alias child-k)
                                (assoc :as-alias true)))))
                ;; shadow-cljs:
                ;; https://shadow-cljs.github.io/docs/UsersGuide.html#_about_default_exports
                :default
                (recur (nnext children)
                       (update m :referred conj (with-meta opt
                                                  (meta opt-expr))))
                :exclude
                (recur
                 (nnext children)
                 (update m :excluded into (set opt)))
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
                (recur (nnext children)
                       m)))
            (let [{:keys [:as :referred :excluded :referred-all :renamed]} m
                  referred (if (and referred-all
                                    (identical? :clj base-lang))
                             (keys (cache/with-cache (:cache-dir ctx) 6
                                     (cache/from-cache-1 (:cache-dir ctx) :clj ns-name)))
                             referred)]
              (when as (lint-alias-consistency ctx ns-name as))
              [{:type :require
                :ns ns-name
                :as as
                :require-kw require-kw
                :excluded excluded
                :referred (concat (map (fn [r]
                                         [r {:ns ns-name
                                             :name r
                                             :filename filename}])
                                       referred)
                                  (map (fn [[original-name new-name]]
                                         [new-name {:ns ns-name
                                                    :name original-name
                                                    :filename filename}])
                                       renamed))
                :referred-all referred-all}])))))))

(defn coerce-class-symbol [ctx node]
  (if-let [v (:value node)]
    (with-meta v
      (meta node))
    (findings/reg-finding!
     ctx
     (node->line (:filename ctx) node :syntax "Expected: class symbol"))))

(defn analyze-import [ctx _ns-name libspec-expr]
  (utils/handle-ignore ctx libspec-expr)
  (case (tag libspec-expr)
    (:vector :list) (let [children (:children libspec-expr)
                          java-package-name-node (first children)
                          java-package (:value java-package-name-node)
                          imported-nodes (rest children)
                          imported (keep #(coerce-class-symbol ctx %) imported-nodes)]
                      (run! #(utils/handle-ignore ctx %) imported-nodes)
                      (when (empty? imported-nodes)
                        (findings/reg-finding!
                         ctx
                         (node->line
                          (:filename ctx) java-package-name-node
                          :syntax "Expected: package name followed by classes.")))
                      (into {} (for [i imported]
                                 [i java-package])))
    :token (let [package+class (:value libspec-expr)
                 splitted (-> package+class name (str/split #"\."))
                 java-package (symbol (str/join "." (butlast splitted)))
                 imported (with-meta (symbol (last splitted))
                            (meta libspec-expr))]
             {imported java-package})
    nil))

(defn analyze-require-clauses [ctx ns-name kw+libspecs]
  (let [lang (:lang ctx)
        analyzed
        (map (fn [[require-kw libspecs]]
               (for [libspec-expr libspecs
                     normalized-libspec-expr (normalize-libspec ctx nil libspec-expr)
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
        required-namespaces (map (fn [req]
                                   (vary-meta (:ns req)
                                              #(assoc % :alias (:as req)))) analyzed)]
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
   :used-vars []
   :unresolved-namespaces {}
   :vars nil
   :row row
   :col col})

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
        doc-node (when docstring
                   fc)
        meta-node (when fc
                    (let [t (tag fc)]
                      (if (= :map t)
                        fc
                        (when-let [sc (second children)]
                          (when (= :map (tag sc))
                            sc)))))
        _ (when meta-node (common/analyze-expression** ctx meta-node))
        meta-node-meta (when meta-node (sexpr meta-node))
        ns-meta (if meta-node-meta
                  (merge metadata meta-node-meta)
                  metadata)
        [doc-node docstring] (or (and meta-node-meta
                                      (:doc meta-node-meta)
                                      (docstring/docs-from-meta meta-node))
                                 [doc-node docstring])
        [doc-node docstring] (if docstring
                               [doc-node docstring]
                               (when (some-> metadata :doc str)
                                 (some docstring/docs-from-meta ns-name-metas)))
        global-config (:global-config ctx)
        ns-name (or
                 (when-let [?name (sexpr ns-name-expr)]
                   (if (symbol? ?name) ?name
                       (findings/reg-finding!
                        ctx
                        (node->line (:filename ctx)
                                    ns-name-expr
                                    :syntax
                                    "namespace name expected"))))
                 'user)
        ns-group (config/ns-group global-config ns-name)
        config-in-ns (let [config-in-ns (:config-in-ns global-config)]
                       (get config-in-ns ns-group))
        local-config (-> ns-meta :clj-kondo/config)
        local-config (if (and (seq? local-config) (= 'quote (first local-config)))
                       (second local-config)
                       local-config)
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
                  munged-ns (str (munge ns-name))]
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
        clauses children
        _ (run! #(utils/handle-ignore ctx %) children)
        kw+libspecs (for [?require-clause clauses
                          :let [require-kw-node (-> ?require-clause :children first)
                                require-kw (:k require-kw-node)
                                require-kw (one-of require-kw [:require :require-macros :use])]
                          :when require-kw]
                      [require-kw-node (-> ?require-clause :children next)])
        analyzed-require-clauses
        (analyze-require-clauses ctx ns-name kw+libspecs)
        imports
        (apply merge
               (for [?import-clause clauses
                     :let [import-kw (some-> ?import-clause :children first :k
                                             (= :import))]
                     :when import-kw
                     libspec-expr (rest (:children ?import-clause))]
                 (analyze-import ctx ns-name libspec-expr)))
        refer-clojure-clauses
        (apply merge-with into
               (for [?refer-clojure (nnext (sexpr expr))
                     :when (= :refer-clojure (first ?refer-clojure))
                     [k v] (partition 2 (rest ?refer-clojure))
                     :let [r (case k
                               :exclude
                               {:excluded (set v)}
                               :rename
                               {:renamed v
                                :excluded (set (keys v))}
                               :only
                               {:only (set v)})]]
                 r))
        refer-clj {:referred-vars
                   (into {} (map (fn [[original-name new-name]]
                                   [new-name {:ns 'clojure.core
                                              :name original-name}])
                                 (:renamed refer-clojure-clauses)))
                   :clojure-excluded (:excluded refer-clojure-clauses)}
        gen-class? (some #(some-> % :children first :k (= :gen-class)) clauses)
        ns (cond->
               (merge (assoc (new-namespace filename base-lang lang ns-name :ns row col)
                             :imports imports
                             :gen-class gen-class?)
                      (merge-with into
                                  analyzed-require-clauses
                                  refer-clj))
             (or config-in-ns local-config) (assoc :config merged-config)
             (identical? :clj lang) (update :qualify-ns
                                            #(assoc % 'clojure.core 'clojure.core))
             (identical? :cljs lang) (update :qualify-ns
                                             #(assoc % 'cljs.core 'cljs.core
                                                     'clojure.core 'cljs.core)))]
    (when (:analysis ctx)
      (when (java/analyze-class-usages? ctx)
        (doseq [[k v] imports]
          (java/reg-class-usage! ctx (str v "." k) (assoc (meta k) :import true))))
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
        children (:children expr)
        require-node (first children)
        children (next children)
        [libspecs non-quoted-children]
        (utils/keep-remove #(let [t (tag %)]
                              (or (when (= :quote t)
                                    (first (:children %)))
                                  (let [children (:children %)]
                                    (when (and (= :list t)
                                               (= 'quote (some-> children first
                                                                 utils/symbol-from-token)))
                                      (second children)))))
                           children)]
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

(comment
  )
