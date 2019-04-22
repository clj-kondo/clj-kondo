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

(defn analyze-fn-body [lang bindings expr]
  (let [children (:children expr)
        arg-list (node/sexpr (first children))
        arg-bindings (extract-bindings arg-list)
        body (rest children)]
    (assoc (analyze-arity arg-list)
           :parsed
           (mapcat #(analyze-expression* lang
                                         (set/union bindings (set arg-bindings)) %)
                   body))))

(defn analyze-defn [lang bindings expr]
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
        parsed-bodies (map #(analyze-fn-body lang bindings %) bodies)
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
    (into [defn call]
          (mapcat :parsed parsed-bodies))))

(defn analyze-case [lang bindings expr]
  (let [exprs (-> expr :children)]
    (loop [[constant expr :as exprs] exprs
           parsed []]
      (if-not expr
        (into parsed (when constant
                       (analyze-expression* lang bindings constant)))
        (recur
         (nnext exprs)
         (into parsed (analyze-expression* lang bindings expr)))))))

(defn analyze-let [lang bindings expr]
  (let [children (:children expr)
        let-bindings (->> children second :children
                          (take-nth 2)
                          (map node/sexpr)
                          (mapcat extract-bindings) set)]
    (mapcat #(analyze-expression* lang (set/union bindings let-bindings) %) (rest children))))

(defn analyze-fn [lang bindings expr]
  ;; TODO better arity analysis like in normal fn
  (let [children (:children expr)
        arg-vec (first (filter #(= :vector (node/tag %)) (rest children)))
        binding-forms (->> arg-vec :children (map node/sexpr))
        ?fn-name (let [n (first children)]
                   (when (symbol? n) n))
        fn-bindings (set (mapcat extract-bindings (cons ?fn-name binding-forms)))]
    (mapcat #(analyze-expression* lang (set/union bindings fn-bindings) %) (rest children))))

(comment
  (parse-string "#{1 2 3}")
  (node/tag (parse-string ":a"))
  


  )

(defn analyze-expression*
  ;; TODO: refactor and split into multiple functions
  ([lang expr] (analyze-expression* lang #{} expr))
  ([lang bindings {:keys [:children] :as expr}]
   (let [t (node/tag expr)]
     (case t
       (:quote :syntax-quote) []
       :map (into (l/lint-map-keys expr)
                  (mapcat #(analyze-expression* lang bindings %) children))
       :set (into (l/lint-set expr)
                  (mapcat #(analyze-expression* lang bindings %) children))
       (let [?full-fn-name (call expr)
             ;; TODO: better resolving for qualified vars...
             fn-name (when ?full-fn-name (symbol (name ?full-fn-name)))]
         (case fn-name
           ns
           [(analyze-ns-decl lang expr)]
           ;; TODO: in-ns is not supported yet
           ;; One thing to note: if in-ns is used in a function body, the rest of the namespace is now analyzed in that namespace, which is incorrect.
           (defn defn- defmacro)
           (analyze-defn lang bindings expr)
           ;; TODO: better resolving for these macro calls
           (->> cond-> cond->> some-> some->> . .. deftype
                proxy extend-protocol doto reify definterface defrecord defprotocol)
           []
           let
           (analyze-let lang bindings expr)
           (fn fn*)
           (analyze-fn lang bindings expr)
           case
           (analyze-case lang bindings expr)
           ;; catch-all
           (if (symbol? fn-name)
             (let [args (count (rest children))
                   binding-call? (contains? bindings fn-name)
                   analyze-rest (mapcat #(analyze-expression* lang bindings %) (rest children))]
               (if binding-call?
                 analyze-rest
                 (cons
                  (let [{:keys [:row :col]} (meta expr)]
                    {:type :call
                     :name ?full-fn-name
                     :arity args
                     :row row
                     :col col
                     :lang lang
                     :expr expr})
                  analyze-rest)))
             (mapcat #(analyze-expression* lang bindings %) children))))))))

(defn resolve-name
  [ns name-sym]
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

(def vconj (fnil conj []))

(defn analyze-expression
  "Analyzes expression and collects defs and calls into a map. To
  optimize cache lookups later on, calls are indexed by the namespace
  they call to, not the ns where the call occurred. Also collects
  other findings and passes them under the :findings key."
  ([filename lang expr] (analyze-expression filename lang lang expr))
  ([filename lang expanded-lang expr] (analyze-expression filename lang expanded-lang expr false))
  ([filename lang expanded-lang expr debug?]
   (loop [[first-parsed & rest-parsed] (analyze-expression* expanded-lang expr)
          ns (analyze-ns-decl expanded-lang (parse-string "(ns user)"))
          results {:calls {}
                   :defs {}
                   :loaded (:loaded ns)
                   :findings []
                   :lang lang}]
     (if first-parsed
       (case (:type first-parsed)
         (:ns :in-ns)
         (recur rest-parsed
                first-parsed
                (update results
                        :loaded into (:loaded first-parsed)))
         (:duplicate-map-key :missing-map-value :duplicate-set-key)
         (recur rest-parsed
                first-parsed
                (update results
                        :findings conj (assoc first-parsed
                                              :filename filename)))
         (recur rest-parsed
                ns
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
       results))))

(defn deep-merge
  "deep merge that also mashes together sequentials"
  ([])
  ([a] a)
  ([a b]
   (cond (and (map? a) (map? b))
         (merge-with deep-merge a b)
         (and (sequential? a) (sequential? b))
         (into a b)
         :else a))
  ([a b & more]
   (apply merge-with deep-merge a b more)))

;;;; processing of string input

(defn analyze-input
  "Analyzes input and returns analyzed defs, calls. Also invokes some
  linters and returns their findings."
  [filename input lang config]
  (try
    (let [parsed (p/parse-string input config)
          nls (l/redundant-let filename parsed)
          ods (l/redundant-do filename parsed)
          findings {:findings (concat nls ods)
                    :lang lang}
          analyzed-expression
          (case lang :cljc
                (let [clj (analyze-expression filename lang
                                              :clj (select-lang parsed :clj)
                                         (:debug config))
                      cljs (analyze-expression filename lang
                                               :cljs (select-lang parsed :cljs)
                                          (:debug config))]
                  (deep-merge clj cljs))
                (analyze-expression filename lang lang parsed (:debug config)))]
      [findings analyzed-expression])
    (catch Exception e
      [{:findings [{:level :error
                    :filename filename
                    :col 0
                    :row 0
                    :message (str "can't parse "
                                  filename ", "
                                  (.getMessage e))}]}])
    (finally
      (when (-> config :output :show-progress)
        (print ".") (flush)))))
