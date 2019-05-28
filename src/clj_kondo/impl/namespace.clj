(ns clj-kondo.impl.namespace
  {:no-doc true}
  (:require
   [clj-kondo.impl.state :as state]
   [clj-kondo.impl.utils :refer [node->line parse-string parse-string-all deep-merge]]
   [clj-kondo.impl.var-info :as var-info]
   [clojure.set :as set]
   [rewrite-clj.node.protocols :as node]
   [rewrite-clj.node.seq :refer [vector-node list-node]]
   [rewrite-clj.node.token :refer [token-node]]
   [clj-kondo.impl.var-info :as var-info]))

;; we store all seen namespaces here, so we could resolve in the call linter,
;; instead of too early, because of in-ns.

(defonce namespaces (atom {}))

(defn reg-namespace!
  "Registers namespace. Deep-merges with already registered namespaces
  with the same name. Returns updated namespace."
  [lang expanded-lang ns]
  (let [path [lang expanded-lang (:name ns)]]
    (get-in (swap! namespaces update-in
                   path deep-merge ns)
            path)))

(defn reg-var!
  [ctx ns-sym var-sym expr]
  (let [lang (:base-lang ctx)
        expanded-lang (:lang ctx)
        path [lang expanded-lang ns-sym]]
    (swap! namespaces update-in path
           (fn [ns]
             (let [vars (:vars ns)]
               (when-let [redefined-ns
                          (or (when (contains? vars var-sym)
                                ns-sym)
                              (when-let [qv (get (:qualify-var ns) var-sym)]
                                (:ns qv))
                              (let [core-ns (case expanded-lang
                                              :clj 'clojure.core
                                              :cljs 'cljs.core)]
                                (when (and (not= ns-sym core-ns)
                                           (not (contains? (:clojure-excluded ns) var-sym))
                                           (var-info/core-sym? expanded-lang var-sym))
                                  core-ns)))]
                 (state/reg-finding!
                  (node->line (:filename ctx)
                              expr :warning
                              :redefined-var
                              (if (= ns-sym redefined-ns)
                                (str "redefined var #'" redefined-ns "/" var-sym)
                                (str var-sym " already refers to #'" redefined-ns "/" var-sym))))))
             (update ns :vars conj var-sym)))))

(defn reg-usage!
  "Registers usage of required namespaced in ns."
  [base-lang lang ns-sym required-ns-sym]
  (swap! namespaces update-in [base-lang lang ns-sym :used]
         conj required-ns-sym))

(defn reg-alias!
  [base-lang lang ns-sym alias-sym aliased-ns-sym]
  (swap! namespaces assoc-in [base-lang lang ns-sym :qualify-ns alias-sym]
         aliased-ns-sym))

(defn reg-binding!
  [base-lang lang ns-sym binding]
  (swap! namespaces update-in [base-lang lang ns-sym :bindings]
         conj binding))

(defn reg-used-binding!
  [base-lang lang ns-sym binding]
  (swap! namespaces update-in [base-lang lang ns-sym :used-bindings]
         conj binding))

(defn list-namespaces []
  (for [[_base-lang m] @namespaces
        [_lang nss] m
        [_ns-name ns] nss]
    ns))

(defn get-namespace [lang expanded-lang ns-sym]
  (get-in @namespaces [lang expanded-lang ns-sym]))

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

(defn- normalize-libspec
  "Adapted from clojure.tools.namespace."
  [prefix libspec-expr]
  (let [children (:children libspec-expr)
        form (node/sexpr libspec-expr)]
    (cond (prefix-spec? form)
          (mapcat (fn [f]
                    (normalize-libspec
                     (symbol (str (when prefix (str prefix "."))
                                  (first form)))
                     f))
                  (rest children))
          (option-spec? form)
          [(with-meta
             (vector-node (into (normalize-libspec prefix (first children)) (rest children)))
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
  (let [libspec (node/sexpr libspec-expr)]
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

(def default-java-imports
  (reduce (fn [acc [prefix sym]]
            (let [fq (symbol (str prefix sym))]
              (-> acc
                  (assoc fq fq)
                  (assoc sym fq))))
          {}
          (into (mapv vector (repeat "java.lang.") '[Boolean Byte CharSequence Character Double
                                                     Integer Long Math String System Thread])
                (mapv vector (repeat "java.math.") '[BigDecimal BigInteger]))))

(defn analyze-ns-decl [{:keys [:base-lang :lang] :as ctx} expr]
  (let [children (:children expr)
        ns-name (or
                 (let [name-expr (second children)]
                   (when-let [?name (node/sexpr name-expr)]
                     (if (symbol? ?name) ?name
                         (state/reg-finding!
                          (node->line (:filename ctx)
                                      name-expr
                                      :error
                                      :ns-syntax
                                      "namespace name expected")))))
                 'user)
        clauses (next children)
        clauses
        (for [?require-clause clauses
              :let [require-kw (some-> ?require-clause :children first :k
                                       #{:require :require-macros})]
              :when require-kw
              libspec-expr (rest (:children ?require-clause)) ;; TODO: fix meta
              normalized-libspec-expr (normalize-libspec nil libspec-expr)
              analyzed (analyze-libspec ctx ns-name require-kw normalized-libspec-expr)]
          analyzed)
        refer-alls (reduce (fn [acc clause]
                             (if (:referred-all clause)
                               (assoc acc (:ns clause) (:excluded clause))
                               acc))
                           {}
                           clauses)
        ns (cond->
               {:type :ns
                :lang lang
                :name ns-name
                :bindings #{}
                :used-bindings #{}
                :vars #{}
                :required (map :ns clauses)
                :qualify-var (into {} (mapcat :referred clauses))
                :qualify-ns (reduce (fn [acc sc]
                                      (cond-> (assoc acc (:ns sc) (:ns sc))
                                        (:as sc)
                                        (assoc (:as sc) (:ns sc))))
                                    {}
                                    clauses)
                :clojure-excluded (set (for [?refer-clojure (nnext (node/sexpr expr))
                                             :when (= :refer-clojure (first ?refer-clojure))
                                             [k v] (partition 2 (rest ?refer-clojure))
                                             :when (= :exclude k)
                                             sym v]
                                         sym))
                :refer-alls refer-alls
                :used (-> (case lang
                            :clj '#{clojure.core}
                            :cljs '#{cljs.core})
                          (into (keys refer-alls))
                          (conj ns-name))}
             (= :clj lang) (update :qualify-ns
                                   #(assoc % 'clojure.core 'clojure.core))
             (= :cljs lang) (update :qualify-ns
                                    #(assoc % 'cljs.core 'cljs.core
                                            'clojure.core 'cljs.core)))]
    (reg-namespace! base-lang lang ns)
    ns))

(defn resolve-name
  [ns name-sym]
  (if-let [ns* (namespace name-sym)]
    (let [ns-sym (symbol ns*)]
      (if-let [ns* (or (get (:qualify-ns ns) ns-sym)
                       ;; referring to the namespace we're in
                       (when (= (:name ns) ns-sym)
                         ns-sym))]
        {:ns ns*
         :name (symbol (name name-sym))}
        (when (= :clj (:lang ns))
          (when-let [ns* (get default-java-imports ns-sym)]
            {:java-interop? true
             :ns ns*
             :name (symbol (name name-sym))}))))
    (or
     (get (:qualify-var ns)
          name-sym)
     (when (contains? (:vars ns) name-sym)
       {:ns (:name ns)
        :name name-sym})
     (let [clojure-excluded? (contains? (:clojure-excluded ns)
                                        name-sym)
           namespace (:name ns)
           core-sym? (when-not clojure-excluded?
                       (var-info/core-sym? (:lang ns) name-sym))
           special-form? (contains? var-info/special-forms name-sym)]
       (if (or core-sym? special-form?)
         {:ns (case (:lang ns)
                :clj 'clojure.core
                :cljs 'cljs.core)
          :name name-sym}
         {:ns namespace
          :name name-sym
          :unqualified? true
          :clojure-excluded? clojure-excluded?})))))

;;;; Scratch

(comment
  (analyze-ns-decl :clj (parse-string (slurp "/tmp/ns.clj")))
  (:used (analyze-ns-decl :clj (parse-string (slurp "/tmp/nsform.clj"))))
  (:java-imports (analyze-ns-decl :clj (parse-string (slurp "/tmp/nsform.clj"))))
  )
