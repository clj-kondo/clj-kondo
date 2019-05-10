(ns clj-kondo.impl.namespace
  {:no-doc true}
  (:require
   [clj-kondo.impl.state :as state]
   [clj-kondo.impl.utils :refer [node->line parse-string parse-string-all]]
   [clj-kondo.impl.var-info :as var-info]
   [clojure.set :as set]
   [rewrite-clj.node.protocols :as node]
   [rewrite-clj.node.seq :refer [vector-node list-node]]
   [rewrite-clj.node.token :refer [token-node]]))

;; we store all seen namespaces here, so we could resolve in the call linter,
;; instead of too early, because of in-ns. this is not yet implemented.

(defonce namespaces (atom {}))

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
  ;; (println libspec-expr)
  (let [children (:children libspec-expr)
        form (node/sexpr libspec-expr)]
    ;;(println "FORM" form)
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

(defn analyze-libspec [{:keys [:lang :filename]} libspec-expr]
  (let [libspec (node/sexpr libspec-expr)]
    (if (symbol? libspec)
      [{:type :require
        :ns (with-meta libspec
              (assoc (meta libspec-expr)
                     :filename filename))}]
      (let [[ns-name & options] libspec
            ns-name (symbol ns-name)
            ;; CLJS clojure namespace aliasing
            ns-name (if (= :cljs lang)
                      (case ns-name
                        clojure.test 'cljs.test
                        clojure.pprint 'cljs.pprint
                        ns-name)
                      ns-name)
            ns-name (with-meta ns-name
                      (assoc (meta (first (:children libspec-expr)))
                             :filename filename))]
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
                 (cond (sequential? opt)
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

(defn analyze-ns-decl [{:keys [:lang] :as ctx} expr]
  (let [children (:children expr)
        clauses (next children)
        clauses
        (for [?require-clause clauses
              ;; :let [_ (println ">" (some-> ?require-clause :children first node/sexpr))]
              :when (contains? #{:require :require-macros}
                               (some-> ?require-clause :children first node/sexpr))
              libspec-expr (rest (:children ?require-clause)) ;; TODO: fix meta
              ;; :let [_ (prn ">" (meta libspec-expr))]
              normalized-libspec-expr (normalize-libspec nil libspec-expr)
              analyzed (analyze-libspec ctx normalized-libspec-expr)]
          analyzed)
        refer-alls (reduce (fn [acc clause]
                             (if (:referred-all clause)
                               (assoc acc (:ns clause) (:excluded clause))
                               acc))
                           {}
                           clauses)]
    (cond->
        {:type :ns
         :lang lang
         :name (or
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
         :used (into
                    (case lang
                      :clj '#{clojure.core}
                      :cljs '#{cljs.core})
                    (keys refer-alls))}
      (= :clj lang) (update :qualify-ns
                            #(assoc % 'clojure.core 'clojure.core))
      (= :cljs lang) (update :qualify-ns
                             #(assoc % 'cljs.core 'cljs.core)))))

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
     (let [clojure-excluded? (contains? (:clojure-excluded ns)
                                        name-sym)
           namespace (:name ns)
           core-sym? (when-not clojure-excluded?
                       (contains? var-info/core-syms name-sym))
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
  (analyze-libspec :clj (node/sexpr (parse-string "[foo.core :refer :all :exclude [foo] :rename {old-name new-name}]")))
  
  )
