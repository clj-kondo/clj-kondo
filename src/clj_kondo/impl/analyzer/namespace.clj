(ns clj-kondo.impl.analyzer.namespace
  {:no-doc true}
  (:require
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.metadata :as meta]
   [clj-kondo.impl.namespace :as namespace]
   [clj-kondo.impl.utils :refer [node->line parse-string
                                 parse-string-all deep-merge
                                 one-of tag sexpr vector-node list-node
                                 token-node]]
   [clojure.set :as set]
   [clojure.string :as str]))

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
          (mapcat (fn [f]
                    (normalize-libspec ctx
                                       (symbol (str (when prefix (str prefix "."))
                                                    (first form)))
                                       f))
                  (rest children))
          (option-spec? form)
          [(with-meta
             (vector-node (into (normalize-libspec ctx prefix (first children)) (rest children)))
             (meta libspec-expr))]
          (valid-ns-name? form)
          [(with-meta (token-node (symbol (str (when prefix (str prefix ".")) form)))
             (meta libspec-expr))]
          (keyword? form)  ; Some people write (:require ... :reload-all)
          nil
          :else
          (throw (ex-info "Unparsable namespace form"
                          {:reason ::unparsable-ns-form
                           :form form})))))

(defn analyze-libspec [{:keys [:base-lang :lang :filename]} current-ns-name require-kw libspec-expr]
  (let [libspec (sexpr libspec-expr)]
    (if (symbol? libspec)
      [{:type :require
        :ns (with-meta libspec
              (assoc (meta libspec-expr)
                     :filename filename))}]
      (let [[ns-name & options] libspec
            ns-name (symbol ns-name)
            ns-name (if (= :cljs lang)
                      (case ns-name
                        clojure.test 'cljs.test
                        clojure.pprint 'cljs.pprint
                        ns-name)
                      ns-name)
            ns-name (with-meta ns-name
                      (assoc (meta (first (:children libspec-expr)))
                             :filename filename))
            self-require? (and
                           (= :cljc base-lang)
                           (= :cljs lang)
                           (= current-ns-name ns-name)
                           (= :require-macros require-kw))]
        (loop [children options
               {:keys [:as :referred :excluded
                       :referred-all :renamed] :as m}
               {:as nil
                :referred #{}
                :excluded #{}
                :referred-all false
                :renamed {}}]
          (if-let [child (first children)]
            (let [opt (fnext children)]
              ;; (println "OPT" opt)
              (case child
                (:refer :refer-macros)
                (recur
                 (nnext children)
                 (cond (and (not self-require?) (sequential? opt))
                       (update m :referred into opt)
                       (= :all opt)
                       (assoc m :referred-all true)
                       :else m))
                :as (recur
                     (nnext children)
                     (assoc m :as opt))
                ;; shadow-cljs:
                ;; https://shadow-cljs.github.io/docs/UsersGuide.html#_about_default_exports
                :default
                (recur (nnext children)
                       (update m :referred conj opt))
                :exclude
                (recur
                 (nnext children)
                 (update m :excluded into (set opt)))
                :rename
                (recur
                 (nnext children)
                 (-> m (update :renamed merge opt)
                     ;; for :refer-all we need to know the excluded
                     (update :excluded into (set (keys opt)))
                     ;; for :refer it is sufficient to pretend they were never referred
                     (update :referred set/difference (set (keys opt)))))
                (recur (nnext children)
                       m)))
            [{:type :require
              :ns ns-name
              :as as
              :excluded excluded
              :referred (concat (map (fn [refer]
                                       [refer {:ns ns-name
                                               :name refer}])
                                     referred)
                                (map (fn [[original-name new-name]]
                                       [new-name {:ns ns-name
                                                  :name original-name}])
                                     renamed))
              :referred-all referred-all}]))))))

(defn analyze-java-import [_ctx _ns-name libspec-expr]
  (case (tag libspec-expr)
    (:vector :list) (let [children (:children libspec-expr)
                          java-package-name-node (first children)
                          java-package (:value java-package-name-node)
                          imported (map :value (rest children))]
                      (into {} (for [i imported]
                                 [i java-package])))
    :token (let [package+class (:value libspec-expr)
                 splitted (-> package+class name (str/split #"\."))
                 java-package (symbol (str/join "." (butlast splitted)))
                 imported (symbol (last splitted))]
             {imported java-package})
    nil))

(defn analyze-require-clauses [{:keys [:lang] :as ctx} ns-name kw+libspecs]
  (let [analyzed (for [[require-kw libspecs] kw+libspecs
                       libspec-expr libspecs
                       normalized-libspec-expr (normalize-libspec ctx nil libspec-expr)
                       analyzed (analyze-libspec ctx ns-name require-kw normalized-libspec-expr)]
                   analyzed)
        refer-alls (reduce (fn [acc clause]
                             (if (:referred-all clause)
                               (assoc acc (:ns clause) (:excluded clause))
                               acc))
                           {}
                           analyzed)]
    {:required (map :ns analyzed)
     :qualify-ns (reduce (fn [acc sc]
                           (cond-> (assoc acc (:ns sc) (:ns sc))
                             (:as sc)
                             (assoc (:as sc) (:ns sc))))
                         {}
                         analyzed)
     :qualify-var (into {} (mapcat :referred analyzed))
     :refer-alls refer-alls
     ;; TODO: rename to :used-namespaces for clarity
     :used
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

(defn analyze-ns-decl [{:keys [:base-lang :lang :findings] :as ctx} expr]
  (let [children (:children expr)
        ns-name-expr (second children)
        ns-name (meta/lift-meta-content2 ctx ns-name-expr)
        metadata (meta ns-name)
        local-config (-> metadata :clj-kondo/config second)
        ns-name (or
                 (let [name-expr (second children)]
                   (when-let [?name (sexpr name-expr)]
                     (if (symbol? ?name) ?name
                         (findings/reg-finding!
                          findings
                          (node->line (:filename ctx)
                                      name-expr
                                      :error
                                      :syntax
                                      "namespace name expected")))))
                 'user)
        clauses (nnext children)
        kw+libspecs (for [?require-clause clauses
                          :let [require-kw (some-> ?require-clause :children first :k
                                                   (one-of [:require :require-macros]))]
                          :when require-kw]
                      [require-kw (-> ?require-clause :children next)])
        analyzed-require-clauses
        (analyze-require-clauses ctx ns-name kw+libspecs)
        java-imports
        (apply merge
               (for [?import-clause clauses
                     :let [import-kw (some-> ?import-clause :children first :k
                                             (= :import))]
                     :when import-kw
                     libspec-expr (rest (:children ?import-clause))]
                 (analyze-java-import ctx ns-name libspec-expr)))
        ns (cond->
               (merge {:type :ns
                       :base-lang base-lang
                       :lang lang
                       :name ns-name
                       :bindings #{}
                       :used-bindings #{}
                       :used-vars []
                       :vars {}
                       :clojure-excluded (set (for [?refer-clojure (nnext (sexpr expr))
                                                    :when (= :refer-clojure (first ?refer-clojure))
                                                    [k v] (partition 2 (rest ?refer-clojure))
                                                    :when (= :exclude k)
                                                    sym v]
                                                sym))
                       :java-imports java-imports}
                      analyzed-require-clauses)
             local-config (assoc :config local-config)
             (= :clj lang) (update :qualify-ns
                                   #(assoc % 'clojure.core 'clojure.core))
             (= :cljs lang) (update :qualify-ns
                                    #(assoc % 'cljs.core 'cljs.core
                                            'clojure.core 'cljs.core)))]
    (namespace/reg-namespace! ctx ns)
    ns))

;;;; Scratch

(comment
  )
