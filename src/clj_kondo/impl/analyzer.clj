(ns clj-kondo.impl.analyzer
  {:no-doc true}
  (:require
   [clj-kondo.impl.config :as config]
   [clj-kondo.impl.linters :as l]
   [clj-kondo.impl.namespace :refer [analyze-ns-decl resolve-name]]
   [clj-kondo.impl.parser :as p]
   [clj-kondo.impl.utils :refer [some-call call node->line
                                 parse-string parse-string-all
                                 tag select-lang]]
   [clj-kondo.impl.metadata :refer [lift-meta]]
   [clj-kondo.impl.macroexpand :as macroexpand]
   [clj-kondo.impl.linters.keys :as key-linter]
   [clojure.set :as set]
   [clojure.string :as str]
   [rewrite-clj.node.protocols :as node]
   [clj-kondo.impl.schema :as schema]
   [clj-kondo.impl.profiler :as profiler]))

(defn extract-bindings [sexpr]
  (cond (and (symbol? sexpr)
             (not= '& sexpr)) [sexpr]
        (vector? sexpr) (mapcat extract-bindings sexpr)
        (map? sexpr)
        (mapcat extract-bindings
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

;;;; function arity

(declare analyze-expression**)

(defn analyze-arity [sexpr]
  ;;(println "ARITY" sexpr)
  (loop [[arg & rest-args] sexpr
         arity 0]
    (if arg
      (if (= '& arg)
        {:min-arity arity
         :varargs? true}
        (recur rest-args
               (inc arity)))
      {:fixed-arity arity})))

(defn analyze-children [filename lang ns bindings children]
  (mapcat #(analyze-expression** filename lang ns bindings %) children))

(defn analyze-fn-body [filename lang ns bindings expr]
  (let [children (:children expr)
        arg-list (node/sexpr (first children))
        arg-bindings (extract-bindings arg-list)
        body (rest children)]
    (assoc (analyze-arity arg-list)
           :parsed
           (analyze-children filename lang ns (set/union bindings (set arg-bindings)) body))))

(defn analyze-defn [filename lang ns bindings expr]
  (let [children (:children expr)
        children (rest children) ;; "my-fn docstring" {:no-doc true} [x y z] x
        name-node (first children)
        fn-name (:value name-node)
        var-meta (meta name-node)
        macro? (or (= 'defmacro (call expr))
                   (:macro var-meta))
        private? (or (= 'defn- (call expr))
                     (:private var-meta))
        bodies (loop [i 0 [expr & rest-exprs :as exprs] (next children)]
                 (let [t (when expr (node/tag expr))]
                   (cond (= :vector t)
                         [{:children exprs}]
                         (= :list t)
                         exprs
                         (not t) []
                         :else (recur (inc i) rest-exprs))))
        parsed-bodies (map #(analyze-fn-body filename lang ns bindings %) bodies)
        fixed-arities (set (keep :fixed-arity parsed-bodies))
        var-args-min-arity (:min-arity (first (filter :varargs? parsed-bodies)))
        {:keys [:row :col]} (meta expr)
        defn
        (if (and fn-name (seq parsed-bodies))
          (cond-> {:type :defn
                   :name fn-name
                   :row row
                   :col col
                   :lang lang}
            macro? (assoc :macro true)
            (seq fixed-arities) (assoc :fixed-arities fixed-arities)
            private? (assoc :private? private?)
            var-args-min-arity (assoc :var-args-min-arity var-args-min-arity))
          {:type :debug
           :level :info
           :message "Could not parse defn form"
           :row row
           :col col
           :lang lang})]
    (cons defn (mapcat :parsed parsed-bodies))))

(defn analyze-case [filename lang ns bindings expr]
  (let [exprs (-> expr :children)]
    (loop [[constant expr :as exprs] exprs
           parsed []]
      (if-not expr
        (into parsed (when constant
                       (analyze-expression** filename lang ns bindings constant)))
        (recur
         (nnext exprs)
         (into parsed (analyze-expression** filename lang ns bindings expr)))))))

(defn expr-bindings [binding-vector]
  (->> binding-vector :children
       (take-nth 2)
       (map node/sexpr)
       (mapcat extract-bindings) set))

(defn lint-even-forms-bindings [form-name expr sexpr]
  (let [num-children (count sexpr)
        {:keys [:row :col]} (meta expr)]
    (when (odd? num-children)
      {:type :invalid-bindings
       :message (format "%s binding vector requires even number of forms" form-name)
       :row row
       :col col
       :level :error})))

(defn analyze-let [filename lang ns bindings expr]
  (let [bv (-> expr :children second)
        bs (expr-bindings bv)]
    (cons
     (lint-even-forms-bindings 'let bv (node/sexpr bv))
     (analyze-children filename lang ns (set/union bindings bs)
                       (rest (:children expr))))))

(defn lint-two-forms-binding-vector [form-name expr sexpr]
  (let [num-children (count sexpr)
        {:keys [:row :col]} (meta expr)]
    (when (not= 2 num-children)
      {:type :invalid-bindings
       :message (format "%s binding vector requires exactly 2 forms" form-name)
       :row row
       :col col
       :level :error})))

(defn analyze-if-let [filename lang ns bindings expr]
  (let [bv (-> expr :children second)
        bs (expr-bindings bv)
        sexpr (node/sexpr bv)]
    (list* (lint-two-forms-binding-vector 'if-let bv sexpr)
           (analyze-children filename lang ns
                             (set/union bindings bs)
                             (rest (:children expr))))))

(defn analyze-when-let [filename lang ns bindings expr]
  (let [bv (-> expr :children second)
        bs (expr-bindings bv)
        sexpr (node/sexpr bv)]
    (list* (lint-two-forms-binding-vector 'when-let bv sexpr)
           (analyze-children filename lang ns
                             (set/union bindings bs)
                             (rest (:children expr))))))

(defn analyze-fn [filename lang ns bindings expr]
  ;; TODO better arity analysis like in normal fn
  (let [children (:children expr)
        arg-vec (first (filter #(= :vector (node/tag %)) (rest children)))
        binding-forms (->> arg-vec :children (map node/sexpr))
        ?fn-name (let [n (first children)]
                   (when (symbol? n) n))
        fn-bindings (set (mapcat extract-bindings (cons ?fn-name binding-forms)))]
    (analyze-children filename lang ns (set/union bindings fn-bindings) children)))

(defn analyze-alias [ns expr]
  (let [[alias-sym ns-sym]
        (map #(-> % :children first :value)
             (rest (:children expr)))]
    (assoc-in ns [:qualify-ns alias-sym] ns-sym)))

(defn analyze-expression**
  ([filename lang ns expr] (analyze-expression** filename lang ns #{} expr))
  ([filename lang ns bindings {:keys [:children] :as expr}]
   (let [t (node/tag expr)
         {:keys [:row :col]} (meta expr)
         arg-count (count (rest children))]
     (case t
       (:quote :syntax-quote) []
       :map (do (key-linter/lint-map-keys filename expr)
                (analyze-children filename lang ns bindings children))
       :set (do (key-linter/lint-set filename expr)
                (analyze-children filename lang ns bindings children))
       :fn (recur filename lang ns bindings (macroexpand/expand-fn expr))
       (let [?full-fn-name (call expr)
             {resolved-namespace :ns
              resolved-name :name
              :keys [:unqualified? :clojure-excluded?] :as res}
             (when ?full-fn-name (resolve-name ns ?full-fn-name))
             resolved-clojure-var-name
             (when (and (not clojure-excluded?)
                        (or unqualified?
                            (= 'clojure.core resolved-namespace)
                            (when (= :cljs lang)
                              (= 'cljs.core resolved-namespace))))
               resolved-name)]
         (case resolved-clojure-var-name
           ns
           (let [ns (analyze-ns-decl lang expr)]
             [ns])
           alias
           [(analyze-alias ns expr)]
           ;; TODO: in-ns is not supported yet
           (defn defn- defmacro)
           (cons {:type :call
                  :name 'defn
                  :row row
                  :col col
                  :lang lang
                  :expr expr
                  :arity arg-count}
                 (analyze-defn filename lang ns bindings (lift-meta filename expr)))
           comment
           (if (-> @config/config :skip-comments) []
               (analyze-children filename lang ns bindings children))

           ->
           (recur filename lang ns bindings (macroexpand/expand-> filename expr))
           ->>
           (recur filename lang ns bindings (macroexpand/expand->> filename expr))
           (cond-> cond->> some-> some->> . .. deftype
                   proxy extend-protocol doto reify definterface defrecord defprotocol)
           []
           let
           (analyze-let filename lang ns bindings expr)
           if-let
           (analyze-if-let filename lang ns bindings expr)
           when-let
           (analyze-when-let filename lang ns bindings expr)
           (fn fn*)
           (analyze-fn filename lang ns bindings expr)
           case
           (analyze-case filename lang ns bindings expr)
           ;; catch-all
           (case [resolved-namespace resolved-name]
             [schema.core defn]
             (cons {:type :call
                    :name 'schema.core/defn
                    :row row
                    :col col
                    :lang lang
                    :expr expr
                    :arity arg-count}
                   (analyze-defn filename lang ns bindings (schema/expand-schema-defn
                                                            (lift-meta filename expr))))
             [cats.core ->=]
             (recur filename lang ns bindings (macroexpand/expand-> filename expr))
             [cats.core ->>=]
             (recur filename lang ns bindings (macroexpand/expand->> filename expr))
             ;; catch-all
             (let [fn-name (when ?full-fn-name (symbol (name ?full-fn-name)))]
               (if (symbol? fn-name)
                 (let [binding-call? (contains? bindings fn-name)
                       analyze-rest (analyze-children filename lang ns bindings (rest children))]
                   (if binding-call?
                     analyze-rest
                     (let [call {:type :call
                                 :name ?full-fn-name
                                 :arity arg-count
                                 :row row
                                 :col col
                                 :lang lang
                                 :expr expr}]
                       (cons call analyze-rest))))
                 (analyze-children filename lang ns bindings children))))))))))

(def vconj (fnil conj []))

(defn analyze-expression*
  [filename lang expanded-lang ns results expression debug?]
  (loop [ns ns
         [first-parsed & rest-parsed :as all] (analyze-expression** filename expanded-lang ns expression)
         results results]
    (if (seq all)
      (case (:type first-parsed)
        nil (recur ns rest-parsed results)
        (:ns :in-ns)
        (recur
         first-parsed
         rest-parsed
         (-> results
             (assoc :ns first-parsed)
             (update
              :loaded into (:loaded first-parsed))))
        (:duplicate-map-key :missing-map-value :duplicate-set-key :invalid-bindings)
        (recur
         ns
         rest-parsed
         (update results
                 :findings conj (assoc first-parsed
                                       :filename filename)))
        ;; catch-all
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
           (let [resolved (resolve-name ns (:name first-parsed))
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
      [ns results])))

(defn analyze-expressions
  "Analyzes expressions and collects defs and calls into a map. To
  optimize cache lookups later on, calls are indexed by the namespace
  they call to, not the ns where the call occurred. Also collects
  other findings and passes them under the :findings key."
  ([filename lang expressions] (analyze-expressions filename lang lang expressions))
  ([filename lang expanded-lang expressions] (analyze-expressions filename lang expanded-lang expressions false))
  ([filename lang expanded-lang expressions debug?]
   (profiler/profile
    :analyze-expressions
    (loop [ns (analyze-ns-decl expanded-lang (parse-string "(ns user)"))
           [expression & rest-expressions] expressions
           results {:calls {}
                    :defs {}
                    :loaded (:loaded ns)
                    :findings []
                    :lang lang}]
      (if expression
        (let [[ns results]
              (analyze-expression* filename lang expanded-lang ns results expression debug?)]
          (recur ns rest-expressions results))
        results)))))

;;;; processing of string input

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
                   (profiler/profile :deep-merge
                                     (deep-merge clj cljs)))
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
