(ns clj-kondo.impl.analyzer
  {:no-doc true}
  (:require
   [clj-kondo.impl.utils :refer [some-call call node->line
                                 parse-string parse-string-all
                                 tag select-lang]]
   [clj-kondo.impl.namespace :refer [analyze-ns-decl]]
   [clojure.set :as set]
   [rewrite-clj.node.protocols :as node]
   [clojure.string :as str]
   [clj-kondo.impl.parser :as p]
   [clj-kondo.impl.linters :as l]))

(defn extract-bindings [sexpr]
  (cond (and (symbol? sexpr)
             (not= '& sexpr)) [sexpr]
        (vector? sexpr) (mapcat extract-bindings sexpr)
        (map? sexpr) (mapcat extract-bindings
                             (for [[k v] sexpr
                                   :let [bindings
                                         (cond (keyword? k)
                                               (case (keyword (name k))
                                                 (:keys :syms :strs)
                                                 (map #(-> % name symbol) v)
                                                 :or (extract-bindings v)
                                                 :as [v])
                                               (symbol? k) [k]
                                               :else nil)]
                                   b bindings]
                               b))
        :else []))

(defn analyze-in-ns [{:keys [:children] :as expr}]
  (let [ns-name (-> children second :children first :value)]
    {:type :in-ns
     :name ns-name}))

(defn fn-call? [expr]
  (let [tag (node/tag expr)]
    (and (= :list tag)
         (symbol? (:value (first (:children expr)))))))

(defn strip-meta* [children]
  (loop [[child & rest-children] children
         stripped []]
    (if child
      (if (contains? '#{:meta :meta*} (node/tag child))
        (recur rest-children
               (into stripped (strip-meta* (rest (:children child)))))
        (recur rest-children
               (conj stripped child)))
      stripped)))

(defn strip-meta [expr]
  (assoc expr
         :children (strip-meta* (:children expr))))

;;;; function arity

(declare analyze-expression*)

(defn analyze-arity [sexpr]
  (loop [[arg & rest-args] sexpr
         arity 0]
    (if arg
      (if (= '& arg)
        {:min-arity arity
         :varargs? true}
        (recur rest-args
               (inc arity)))
      {:fixed-arity arity})))

(defn mapcat-children [lang ns bindings children]
  (mapcat #(analyze-expression* lang ns bindings %) children))

(defn analyze-fn-body [lang ns bindings expr]
  (let [children (:children expr)
        arg-list (node/sexpr (first children))
        arg-bindings (extract-bindings arg-list)
        body (rest children)]
    (assoc (analyze-arity arg-list)
           :parsed
           (mapcat-children lang ns (set/union bindings (set arg-bindings)) body))))

(defn analyze-defn [lang ns bindings expr]
  (let [macro? (= 'defmacro (call expr))
        children (:children (strip-meta expr))
        private? (= 'defn- (call expr))
        children (rest children) ;; "my-fn docstring" {:no-doc true} [x y z] x
        ;; TODO: do we need this still?
        children (strip-meta* children)
        fn-name (:value (first children))
        bodies (loop [i 0 [expr & rest-exprs :as exprs] (next children)]
                 (let [t (when expr (node/tag expr))]
                   (cond (= :vector t)
                         [{:children exprs}]
                         (= :list t)
                         exprs
                         (not t) (throw (Exception. "unexpected error when parsing" (node/sexpr expr)))
                         :else (recur (inc i) rest-exprs))))
        parsed-bodies (map #(analyze-fn-body lang ns bindings %) bodies)
        ;; _ (println "PB" parsed-bodies)
        fixed-arities (set (keep :fixed-arity parsed-bodies))
        var-args-min-arity (:min-arity (first (filter :varargs? parsed-bodies)))
        {:keys [:row :col]} (meta expr)
        defn
        (if fn-name
          (cond-> {:type :defn
                   :name fn-name
                   :row row
                   :col col
                   :lang lang}
            ;; not yet:
            ;; macro? (assoc :macro true)
            (seq fixed-arities) (assoc :fixed-arities fixed-arities)
            private? (assoc :private? private?)
            var-args-min-arity (assoc :var-args-min-arity var-args-min-arity))
          {:type :debug
           :level :info
           :message "Could not parse defn form"
           :row row
           :col col
           :lang lang})
        call {:type :call
              :name 'defn
              :row row
              :col col
              :lang lang
              :expr expr
              :arity (count children)}]
    (list* defn call (mapcat :parsed  parsed-bodies))))

(defn analyze-case [lang ns bindings expr]
  (let [exprs (-> expr :children)]
    (loop [[constant expr :as exprs] exprs
           parsed []]
      (if-not expr
        (into parsed (when constant
                       (analyze-expression* lang ns bindings constant)))
        (recur
         (nnext exprs)
         (into parsed (analyze-expression* lang ns bindings expr)))))))

(defn analyze-let [lang ns bindings expr]
  (let [children (:children expr)
        let-bindings (->> children second :children
                          (take-nth 2)
                          (map node/sexpr)
                          (mapcat extract-bindings) set)]
    (mapcat-children lang ns (set/union bindings let-bindings) (rest children))))

(defn analyze-fn [lang ns bindings expr]
  ;; TODO better arity analysis like in normal fn
  (let [children (:children expr)
        arg-vec (first (filter #(= :vector (node/tag %)) (rest children)))
        binding-forms (->> arg-vec :children (map node/sexpr))
        ?fn-name (let [n (first children)]
                   (when (symbol? n) n))
        fn-bindings (set (mapcat extract-bindings (cons ?fn-name binding-forms)))]
    (mapcat-children lang ns (set/union bindings fn-bindings) children)))

(defn resolve-name
  [ns name-sym]
  ;; (println "NAME SYM" name-sym)
  (if-let [ns* (namespace name-sym)]
    (let [ns-sym (symbol ns*)]
      (if-let [ns* (get (:qualify-ns ns) ns-sym)]
        {:ns ns*
         :name (symbol (name name-sym))}
        (when-let [ns* (get (:java-imports ns) ns-sym)]
          {:java-interop? true
           :ns ns*
           :name (symbol (name name-sym))})))
    (or (get (:qualify-var ns)
             name-sym)
        (let [namespace (:name ns)]
          {:ns namespace
           :name name-sym
           :unqualified? true
           :clojure-excluded? (contains? (:clojure-excluded ns)
                                         name-sym)}))))

#_(defn reduce-children [lang ns children]
    ;; (println "REDUCING CHILDREN" children)
    (reduce (fn [{acc-analyzed :analyzed
                  acc-ns :ns} child]
              (let [{:keys [:ns :analyzed]} (analyze-expression* lang acc-ns child)]
                {:analyzed (concat acc-analyzed analyzed)
                 :ns ns}))
            {:analyzed '()
             :ns ns}
            children))

(defn analyze-expression*
  ;; TODO: refactor and split into multiple functions
  ;; TODO: take namespace into account when parsing defn
  ([lang ns expr] (analyze-expression* lang ns #{} expr))
  ([lang ns bindings {:keys [:children] :as expr}]
   (let [t (node/tag expr)]
     (case t
       (:quote :syntax-quote) []
       :map (concat (l/lint-map-keys expr) (mapcat-children lang ns bindings children))
       :set (concat (l/lint-set expr) (mapcat-children lang ns bindings children))
       (let [;; _ (println "EXPR" expr)
             ?full-fn-name (call expr)
             ;; TODO: better resolving for qualified vars...
             fn-name (when ?full-fn-name (symbol (name ?full-fn-name)))
             {resolved-namespace :ns
              resolved-name :name
              :keys [:unqualified? :clojure-excluded?] :as res} (when ?full-fn-name (resolve-name ns ?full-fn-name))
             ;; _ (println (meta expr))
             ;; _ (when-not res (prn "RESOLVED" ?full-fn-name res (:ns ns)) (meta expr))
             ;; _ (println "RESOLVED NS" resolved-namespace)
             resolved-fn-name (when (and (not clojure-excluded?)
                                         (or unqualified?
                                             (= 'clojure.core resolved-namespace)
                                             (when (= :cljs lang)
                                               (= 'cljs.core resolved-namespace))))
                                resolved-name)]
         #_(when fn-name-old (println "FN-NAME-OLD" fn-name-old))
         #_(println "FULL" ?full-fn-name "NS" (:name ns) "FN-NAME" fn-name "META..." (meta expr))
         (case resolved-fn-name
           ns
           (let [ns (analyze-ns-decl lang expr)]
             [ns])
           ;; TODO: in-ns is not supported yet
           ;; One thing to note: if in-ns is used in a function body, the rest of the namespace is now analyzed in that namespace, which is incorrect.
           (defn defn- defmacro)
           (analyze-defn lang ns bindings expr)
           ;; TODO: better resolving for these macro calls
           (->> cond-> cond->> some-> some->> . .. deftype
                proxy extend-protocol doto reify definterface defrecord defprotocol)
           []
           let
           (analyze-let lang ns bindings expr)
           (fn fn*)
           (analyze-fn lang ns bindings expr)
           case
           (analyze-case lang ns bindings expr)
           ;; catch-all
           (if (symbol? fn-name)
             (let [args (count (rest children))
                   binding-call? (contains? bindings fn-name)
                   ;; _ (println "BINDING CALL?" binding-call?)
                   analyze-rest (mapcat-children lang ns bindings (rest children))
                   ;; _ (println "ANALUZE-REST" (count (:analyzed analyze-rest)))
                   ]
               (if binding-call?
                 analyze-rest
                 (let [call (let [{:keys [:row :col]} (meta expr)]
                              {:type :call
                               :name ?full-fn-name
                               :arity args
                               :row row
                               :col col
                               :lang lang
                               :expr expr})]
                   (cons call analyze-rest))))
             ;; here we provide the wrong ns to the rest of the expressions.
             (do
               ;; (println ">>")
               (mapcat-children lang ns bindings children)))))))))

(comment
  (analyze-expression*
   :clj
   (analyze-ns-decl :clj (parse-string "(ns foo (:require [schema :as s]))"))
   (parse-string "(s/defn foo [])"))
  (analyze-expression*
   :clj
   (analyze-ns-decl :clj (parse-string "(ns foo)"))
   (parse-string "(defn foo [])"))
  (analyze-expression*
   :clj
   (analyze-ns-decl :clj (parse-string "(ns foo (:refer-clojure :exclude [defn]) (:require [clojure.core :as c]))"))
   (parse-string "(c/defn foo [])"))

  (resolve-name (analyze-ns-decl :clj (parse-string "(ns cond-without-else1
  (:refer-clojure :exclude [cond])
  (:require [clojure.core :as c]))")) 'c/cond)

  )

(def vconj (fnil conj []))

(defn deep-merge
  "deep merge that also mashes together sequentials"
  ([])
  ([a] a)
  ([a b]
   (cond (and (map? a) (map? b))
         (merge-with deep-merge a b)
         (and (sequential? a) (sequential? b))
         (into a b)
         (and (set? a) (set? b))
         (into a b)
         :else a))
  ([a b & more]
   (apply merge-with deep-merge a b more)))

(defn analyze-expressions
  "Analyzes expressions and collects defs and calls into a map. To
  optimize cache lookups later on, calls are indexed by the namespace
  they call to, not the ns where the call occurred. Also collects
  other findings and passes them under the :findings key."
  ([filename lang expressions] (analyze-expressions filename lang lang expressions))
  ([filename lang expanded-lang expressions] (analyze-expressions filename lang expanded-lang expressions false))
  ([filename lang expanded-lang expressions debug?]
   ;; (println "---")
   (loop [ns (analyze-ns-decl expanded-lang (parse-string "(ns user)"))
          [expression & rest-expressions] expressions
          results {:calls {}
                   :defs {}
                   :loaded (:loaded ns)
                   :findings []
                   :lang lang}]
     (if expression
       (let [[ns results]
             (loop [ns ns
                    [first-parsed & rest-parsed] (analyze-expression* expanded-lang ns expression)
                    results (assoc results :ns ns)]
               #_(println "FIRST PARSED" (when (= :call (:type first-parsed))
                                           first-parsed) "RESULTS" (:name (:ns results)))
               (if first-parsed
                 (case (:type first-parsed)
                   (:ns :in-ns)
                   (recur
                    first-parsed
                    rest-parsed
                    (-> results
                        (assoc :ns first-parsed)
                        (update
                         :loaded into (:loaded first-parsed))))
                   (:duplicate-map-key :missing-map-value :duplicate-set-key)
                   (recur
                    ns
                    rest-parsed
                    (update results
                            :findings conj (assoc first-parsed
                                                  :filename filename)))
                   (recur
                    ns
                    rest-parsed
                    (case (:type first-parsed)
                      :debug
                      (if debug?
                        (update-in results
                                   [:findings]
                                   conj
                                   (assoc first-parsed
                                          :filename filename))
                        results)
                      (let [;; _ (println "FIRST PARSED" first-parsed)
                            resolved (resolve-name ns (:name first-parsed))
                            first-parsed (cond->
                                             (assoc first-parsed
                                                    :name (:name resolved)
                                                    :ns (:name ns))
                                           ;; if defined in CLJC file, we add that as the base-lang
                                           (= :cljc lang)
                                           (assoc :base-lang lang))]
                        (case (:type first-parsed)
                          :defn
                          (let [path (case lang
                                       :cljc [:defs (:name ns) (:lang first-parsed) (:name resolved)]
                                       [:defs (:name ns) (:name resolved)])
                                results
                                (if resolved
                                  (assoc-in results path
                                            (dissoc first-parsed
                                                    :type))
                                  results)]
                            (if debug?
                              (update-in results
                                         [:findings]
                                         vconj
                                         (assoc first-parsed
                                                :level :info
                                                :filename filename
                                                :message
                                                (str/join " "
                                                          ["Defn resolved as"
                                                           (str (:ns resolved) "/" (:name resolved)) "with arities"
                                                           "fixed:"(:fixed-arities first-parsed)
                                                           "varargs:"(:var-args-min-arity first-parsed)])
                                                :type :debug))
                              results))
                          :call
                          (if resolved
                            (let [path [:calls (:ns resolved)]
                                  unqualified? (:unqualified? resolved)
                                  call (cond-> (assoc first-parsed
                                                      :filename filename
                                                      :resolved-ns (:ns resolved)
                                                      :ns-lookup ns)
                                         (:clojure-excluded? resolved)
                                         (assoc :clojure-excluded? true)
                                         unqualified?
                                         (assoc :unqualified? true))
                                  results (cond-> (update-in results path vconj call)
                                            (not unqualified?)
                                            (update :loaded conj (:ns resolved)))]
                              (if debug? (update-in results [:findings] conj
                                                    (assoc call
                                                           :level :info
                                                           :message (str "Call resolved as "
                                                                         (str (:ns resolved) "/" (:name resolved)))
                                                           :type :debug))
                                  results))
                            (if debug?
                              (update-in results
                                         [:findings]
                                         conj
                                         (assoc first-parsed
                                                :level :info
                                                :message (str "Unrecognized call to "
                                                              (:name first-parsed))
                                                :type :debug))
                              results))
                          results)))))
                 [ns results]))]
         ;; (println "RESULTS* NS" (:ns results*))
         (recur ns rest-expressions results))
       results))))

;;;; processing of string input

(defn analyze-input
  "Analyzes input and returns analyzed defs, calls. Also invokes some
  linters and returns their findings."
  [filename input lang config dev?]
  (try
    (let [parsed (p/parse-string input config)
          nls (l/redundant-let filename parsed)
          ods (l/redundant-do filename parsed)
          findings {:findings (concat nls ods)
                    :lang lang}
          analyzed-expressions
          (case lang :cljc
                (let [clj (analyze-expressions filename lang
                                               :clj (:children (select-lang parsed :clj))
                                               (:debug config))
                      cljs (analyze-expressions filename lang
                                                :cljs (:children (select-lang parsed :cljs))
                                                (:debug config))]
                  (deep-merge clj cljs))
                (analyze-expressions filename lang lang
                                     (:children parsed)
                                     (:debug config)))]
      [findings analyzed-expressions])
    (catch Exception e
      (if dev? (throw e)
          [{:findings [{:level :error
                        :filename filename
                        :col 0
                        :row 0
                        :message (str "can't parse "
                                      filename ", "
                                      (.getMessage e))}]}]))
    (finally
      (when (-> config :output :show-progress)
        (print ".") (flush)))))
