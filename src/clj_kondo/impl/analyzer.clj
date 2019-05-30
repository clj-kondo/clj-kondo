(ns clj-kondo.impl.analyzer
  {:no-doc true}
  (:require
   [clj-kondo.impl.config :as config]
   [clj-kondo.impl.linters.keys :as key-linter]
   [clj-kondo.impl.macroexpand :as macroexpand]
   [clj-kondo.impl.metadata :as meta :refer [lift-meta]]
   [clj-kondo.impl.namespace :as namespace :refer [analyze-ns-decl resolve-name]]
   [clj-kondo.impl.node.seq] ;; load defrecord
   [clj-kondo.impl.parser :as p]
   [clj-kondo.impl.profiler :as profiler]
   [clj-kondo.impl.schema :as schema]
   [clj-kondo.impl.state :as state]
   [clj-kondo.impl.utils :as utils :refer [some-call symbol-call keyword-call node->line
                                           parse-string parse-string-all tag select-lang
                                           vconj deep-merge one-of]]
   [clojure.string :as str]
   [rewrite-clj.node.protocols :as node]
   [rewrite-clj.node.seq :as seq]
   [rewrite-clj.node.token :as token])
  (:import [clj_kondo.impl.node.seq NamespacedMapNode]))

(declare analyze-expression**)

(defn analyze-children [{:keys [:callstack] :as ctx} children]
  (when-not (config/skip? callstack)
    (mapcat #(analyze-expression** ctx %) children)))

(defn extract-bindings
  ([ctx expr] (extract-bindings ctx expr false))
  ([ctx expr keys-destructuring?]
   (let [expr (meta/lift-meta-content ctx expr)
         t (node/tag expr)]
     (case t
       :token
       (cond
         ;; symbol
         (utils/symbol-token? expr)
         (let [expr (meta/lift-meta-content ctx expr)
               sym (:value expr)]
           (when (not= '& sym)
             (let [ns (namespace sym)
                   valid? (or (not ns)
                              keys-destructuring?)]
               (if valid?
                 (let [s (symbol (name sym))
                       m (meta expr)
                       v (assoc m
                                :name s
                                :filename (:filename ctx))]
                   (namespace/reg-binding! (:base-lang ctx)
                                           (:lang ctx)
                                           (-> ctx :ns :name)
                                           (assoc m
                                                  :name s
                                                  :filename (:filename ctx)))
                   {s v})
                 (state/reg-finding! (node->line (:filename ctx)
                                                 expr
                                                 :error
                                                 :unsupported-binding-form
                                                 (str "unsupported binding form " sym)))))))
         ;; keyword
         (:k expr)
         (let [k (:k expr)]
           (when (not= :as k)
             (if keys-destructuring?
               (let [s (-> expr :k name symbol)
                     m (meta expr)
                     v (assoc m
                              :name s
                              :filename (:filename ctx))]
                 (namespace/reg-binding! (:base-lang ctx)
                                         (:lang ctx)
                                         (-> ctx :ns :name)
                                         v)
                 {s v})
               (state/reg-finding! (node->line (:filename ctx)
                                               expr
                                               :error
                                               :unsupported-binding-form
                                               (str "unsupported binding form " (:k expr)))))))
         :else
         (state/reg-finding!
          (node->line (:filename ctx)
                      expr
                      :error
                      :unsupported-binding-form
                      (str "unsupported binding form " expr))))
       :vector (into {} (map #(extract-bindings ctx %)) (:children expr))
       :namespaced-map (extract-bindings ctx (first (:children expr)))
       :map
       (into {}
             (for [[k v] (partition 2 (:children expr))]
               (cond (:k k)
                     (case (keyword (name (:k k)))
                       (:keys :syms :strs) (into {} (map #(extract-bindings ctx % true))
                                                 (:children v))
                       ;; or doesn't introduce new bindings, it only gives defaults
                       :or {:analyzed (analyze-children ctx (utils/map-node-vals v))}
                       :as (extract-bindings ctx v)
                       nil)
                     (utils/symbol-token? k) (extract-bindings ctx k)
                     :else nil)))
       (state/reg-finding!
        (node->line (:filename ctx)
                    expr
                    :error
                    :unsupported-binding-form
                    (str "unsupported binding form " expr)))))))

(defn analyze-in-ns [ctx {:keys [:children] :as _expr}]
  (let [ns-name (-> children second :children first :value)
        ns {:type :in-ns
            :name ns-name
            :lang (:lang ctx)
            :vars #{}
            :used #{}
            :bindings #{}
            :used-bindings #{}}]
    (namespace/reg-namespace! (:base-lang ctx) (:lang ctx) ns)
    ns))

(defn fn-call? [expr]
  (let [tag (node/tag expr)]
    (and (= :list tag)
         (symbol? (:value (first (:children expr)))))))

;;;; function arity

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

(defn analyze-fn-arity [ctx body]
  (let [children (:children body)
        arg-vec  (first children)
        arg-list (node/sexpr arg-vec)
        ;; TODO: extract-bindings also extracts bindings from keywords
        ;; prevent it here?
        arg-bindings (extract-bindings ctx arg-vec)
        arity (analyze-arity arg-list)]
    {:arg-bindings (dissoc arg-bindings :analyzed)
     :arity arity
     :analyzed-arg-vec (:analyzed arg-bindings)}))

(defn analyze-fn-body [{:keys [bindings] :as ctx} body]
  (let [{:keys [:arg-bindings
                :arity :analyzed-arg-vec]} (analyze-fn-arity ctx body)
        children (:children body)
        body-exprs (rest children)
        parsed
        (analyze-children
         (assoc ctx
                :bindings (merge bindings arg-bindings)
                :recur-arity arity
                :fn-body true) body-exprs)]
    (assoc arity
           :parsed
           (concat analyzed-arg-vec parsed))))

(defn fn-bodies [children]
  (loop [i 0 [expr & rest-exprs :as exprs] children]
    (let [t (when expr (node/tag expr))]
      (cond (= :vector t)
            [{:children exprs}]
            (= :list t)
            exprs
            (not t) []
            :else (recur (inc i) rest-exprs)))))

(defn analyze-defn [{:keys [base-lang lang] :as ctx} expr]
  (let [children (:children expr)
        children (rest children) ;; "my-fn docstring" {:no-doc true} [x y z] x
        name-node (first children)
        fn-name (:value name-node)
        var-meta (meta name-node)
        call-sym (symbol-call expr)
        macro? (or (= 'defmacro call-sym)
                   (:macro var-meta))
        private? (or (= 'defn- call-sym)
                     (:private var-meta))
        bodies (fn-bodies (next children))
        parsed-bodies (map #(analyze-fn-body ctx %) bodies)
        fixed-arities (set (keep :fixed-arity parsed-bodies))
        var-args-min-arity (:min-arity (first (filter :varargs? parsed-bodies)))
        {:keys [:row :col]} (meta expr)
        defn
        (if fn-name
          (cond-> {:type :defn
                   :name fn-name
                   :row row
                   :col col
                   :base-lang base-lang
                   :lang lang
                   :expr expr}
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

(defn analyze-case [ctx expr]
  (let [exprs (-> expr :children)]
    (loop [[constant expr :as exprs] exprs
           parsed []]
      (if-not expr
        (into parsed (when constant
                       (analyze-expression** ctx constant)))
        (recur
         (nnext exprs)
         (into parsed (analyze-expression** ctx expr)))))))

(defn expr-bindings [ctx binding-vector]
  (->> binding-vector :children
       (take-nth 2)
       (map #(extract-bindings ctx %))
       (reduce deep-merge {})))

(defn analyze-let-like-bindings [ctx binding-vector]
  (let [call (-> ctx :callstack second second)
        for-like? (one-of call [for doseq])]
    (loop [[binding value & rest-bindings] (-> binding-vector :children)
           bindings (:bindings ctx)
           arities (:arities ctx)
           analyzed []]
      (if binding
        (let [binding-sexpr (node/sexpr binding)
              for-let? (and for-like?
                            (= :let binding-sexpr))]
          (if for-let?
            (let [{new-bindings :bindings
                   new-analyzed :analyzed
                   new-arities :arities}
                  (analyze-let-like-bindings
                   (update ctx :bindings
                           (fn [b]
                             (merge b bindings))) value)]
              (recur rest-bindings
                     (merge bindings new-bindings)
                     (merge arities new-arities)
                     (concat analyzed new-analyzed)))
            (let [binding (cond for-let? value
                                (keyword? binding-sexpr) nil
                                :else binding)
                  new-bindings (when binding (extract-bindings ctx binding))
                  analyzed-binding (:analyzed new-bindings)
                  new-bindings (dissoc new-bindings :analyzed)
                  ctx* (-> ctx
                           (update :bindings (fn [b]
                                               (merge b bindings)))
                           (update :arities merge arities))
                  analyzed-value (when (and value (not for-let?))
                                   (analyze-expression** ctx* value))
                  next-arities (if-let [arity (:arity (meta analyzed-value))]
                                 (assoc arities binding-sexpr arity)
                                 arities)]
              (recur rest-bindings
                     (merge bindings new-bindings)
                     next-arities (concat analyzed analyzed-binding analyzed-value)))))
        {:arities arities
         :bindings bindings
         :analyzed analyzed}))))

(defn lint-even-forms-bindings! [ctx form-name bv]
  (let [num-children (count (:children bv))
        {:keys [:row :col]} (meta bv)]
    (when (odd? num-children)
      (state/reg-finding!
       {:type :invalid-bindings
        :message (format "%s binding vector requires even number of forms" form-name)
        :row row
        :col col
        :level :error
        :filename (:filename ctx)}))))

(defn analyze-like-let
  [{:keys [:filename :callstack
           :lang :base-lang
           :maybe-redundant-let?] :as ctx} expr]
  (let [children (:children expr)
        call (-> callstack first second)
        let? (= 'let call)
        let-parent? (one-of (second callstack)
                            [[clojure.core let]
                             [cljs.core let]])
        bv (-> expr :children second)
        {:keys [:row :col]} (meta expr)
        arg-count (count (rest children))]
    (when (and let? let-parent? maybe-redundant-let?)
      (state/reg-finding! (node->line filename expr :warning :redundant-let "redundant let")))
    (cons {:type :call
           :name call
           :row row
           :col col
           :base-lang base-lang
           :lang lang
           :expr expr
           :arity arg-count}
          (when (and bv (= :vector (node/tag bv)))
            (let [{analyzed-bindings :bindings
                   arities :arities
                   analyzed :analyzed}
                  (analyze-let-like-bindings
                   (-> ctx
                       ;; prevent linting redundant let when using let in bindings
                       (update :callstack #(cons [nil :let-bindings] %))) bv)
                  let-body (nnext (:children expr))
                  single-child? (and let? (= 1 (count let-body)))]
              (lint-even-forms-bindings! ctx 'let bv)
              (concat analyzed
                      (analyze-children
                       (-> ctx
                           (update :bindings (fn [b]
                                               (merge b analyzed-bindings)))
                           (update :arities merge arities)
                           (assoc :maybe-redundant-let? single-child?))
                       let-body)))))))

(defn analyze-do [{:keys [:filename :callstack] :as ctx} expr]
  (let [parent-call (second callstack)
        core? (one-of (first parent-call) [clojure.core cljs.core])
        core-sym (when core?
                   (second parent-call))
        redundant?
        (and (not= 'fn* core-sym)
             (not= 'let* core-sym)
             (or
              ;; zero or one children
              (< (count (rest (:children expr))) 2)
              (and core?
                   (or
                    ;; explicit do
                    (= 'do core-sym)
                    ;; implicit do
                    (one-of core-sym [fn defn defn-
                                      let loop binding with-open
                                      doseq try])))))]
    (when redundant?
      (state/reg-finding! (node->line filename expr :warning :redundant-do "redundant do"))))
  (analyze-children ctx (next (:children expr))))

(defn lint-two-forms-binding-vector! [ctx form-name expr sexpr]
  (let [num-children (count sexpr)
        {:keys [:row :col]} (meta expr)]
    (when (not= 2 num-children)
      (state/reg-finding!
       {:type :invalid-bindings
        :message (format "%s binding vector requires exactly 2 forms" form-name)
        :row row
        :col col
        :filename (:filename ctx)
        :level :error}))))

(defn analyze-if-let [ctx expr]
  (let [callstack (:callstack ctx)
        call (-> callstack first second)
        bv (-> expr :children second)
        sexpr (and bv (node/sexpr bv))]
    (when (vector? sexpr)
      (let [bindings (expr-bindings ctx bv)
            eval-expr (-> bv :children second)
            body-exprs (-> expr :children nnext)]
        (lint-two-forms-binding-vector! ctx call bv sexpr)
        (concat (:analyzed bindings)
                (analyze-expression** ctx eval-expr)
                (analyze-children (update ctx :bindings
                                          (fn [b] (merge b
                                                         (dissoc bindings
                                                                 :analyzed))))
                                  body-exprs))))))

(defn fn-arity [ctx bodies]
  (let [arities (map #(analyze-fn-arity ctx %) bodies)
        fixed-arities (set (keep (comp :fixed-arity :arity) arities))
        var-args-min-arity (some #(when (:varargs? (:arity %))
                                    (:min-arity (:arity %))) arities)]
    (cond-> {}
      (seq fixed-arities) (assoc :fixed-arities fixed-arities)
      var-args-min-arity (assoc :var-args-min-arity var-args-min-arity))))

(defn analyze-fn [ctx expr]
  (let [children (:children expr)
        ?fn-name (when-let [?name-expr (second children)]
                   (let [n (node/sexpr ?name-expr)]
                     (when (symbol? n)
                       n)))
        bodies (fn-bodies (next children))
        ;; we need the arity beforehand because this is valid in each body
        arity (fn-arity ctx bodies)
        parsed-bodies (map #(analyze-fn-body
                             (if ?fn-name
                               (-> ctx
                                   (update :bindings conj [?fn-name
                                                           (assoc (meta (second children))
                                                                  :name ?fn-name
                                                                  :filename (:filename ctx))])
                                   (update :arities assoc ?fn-name
                                           arity))
                               ctx) %) bodies)]
    (with-meta (mapcat :parsed parsed-bodies)
      {:arity arity})))

(defn analyze-alias [ctx expr]
  (let [ns (:ns ctx)
        [alias-sym ns-sym]
        (map #(-> % :children first :value)
             (rest (:children expr)))]
    (namespace/reg-alias! (:base-lang ctx) (:lang ctx) (:name ns) alias-sym ns-sym)
    (assoc-in ns [:qualify-ns alias-sym] ns-sym)))

(defn analyze-loop [ctx expr]
  (let [bv (-> expr :children second)]
    (when (and bv (= :vector (node/tag bv)))
      (let [arg-count (let [c (count (:children bv))]
                        (when (even? c)
                          (/ c 2)))]
        (analyze-like-let (assoc ctx
                                 :recur-arity {:fixed-arity arg-count}) expr)))))

(defn analyze-recur [ctx expr]
  (when-not (:call-as-use ctx)
    (let [arg-count (count (rest (:children expr)))
          recur-arity (-> ctx :recur-arity)
          expected-arity
          (or (:fixed-arity recur-arity)
              ;; var-args must be passed as a seq or nil in recur
              (when-let [min-arity (:min-arity recur-arity)]
                (inc min-arity)))]
      (cond
        (not expected-arity)
        (state/reg-finding! (node->line
                             (:filename ctx)
                             expr
                             :warning
                             :unexpected-recur "unexpected recur"))
        (not= expected-arity arg-count)
        (state/reg-finding!
         (node->line
          (:filename ctx)
          expr
          :error
          :invalid-arity
          (format "recur argument count mismatch (expected %d, got %d)" expected-arity arg-count)))
        :else nil)))
  (analyze-children ctx (:children expr)))

(defn analyze-letfn [ctx expr]
  (let [fns (-> expr :children second :children)
        name-exprs (map #(-> % :children first) fns)
        ctx (update ctx :bindings
                    (fn [b]
                      (into b (map (fn [name-expr]
                                     [(:value name-expr)
                                      (assoc (meta name-expr)
                                             :name (:value name-expr)
                                             :filename (:filename ctx))])
                                   name-exprs))))
        processed-fns (for [f fns
                            :let [children (:children f)
                                  fn-name (:value (first children))
                                  bodies (fn-bodies (next children))
                                  arity (fn-arity ctx bodies)]]
                        {:name fn-name
                         :arity arity
                         :bodies bodies})
        ctx (reduce (fn [ctx pf]
                      (assoc-in ctx [:arities (:name pf)]
                                (:arity pf)))
                    ctx processed-fns)
        parsed-fns (map #(analyze-fn-body ctx %) (mapcat :bodies processed-fns))
        analyzed-children (analyze-children ctx (->> expr :children (drop 2)))]
    (concat (mapcat (comp :parsed) parsed-fns) analyzed-children)))

(defn analyze-namespaced-map [ctx ^NamespacedMapNode expr]
  (let [children (:children expr)
        m (first children)
        ns (:ns ctx)
        ns-sym (-> expr :ns :k symbol)
        used (when-let [resolved-ns (get (:qualify-ns ns) ns-sym)]
               [{:type :use
                 :ns resolved-ns}])]
    (concat used (analyze-expression** ctx m))))

(defn analyze-schema-defn [ctx expr]
  (let [arg-count (count (rest (:children expr)))
        {:keys [:base-lang :lang :filename]} ctx
        {:keys [:row :col]} (meta expr)
        {:keys [:defn :schemas]} (schema/expand-schema-defn2
                                  (lift-meta ctx expr))]
    (cons {:type :call
           :name 'schema.core/defn
           :row row
           :col col
           :base-lang base-lang
           :lang lang
           :expr expr
           :arity arg-count}
          (concat
           (namespace/used-namespaces ctx false {:children schemas})
           (analyze-defn ctx defn)))))

(defn analyze-deftest [ctx _deftest-ns expr]
  (let [arg-count (count (rest (:children expr)))
        {:keys [:base-lang :lang]} ctx
        {:keys [:row :col]} (meta expr)]
    (cons {:type :call
           :name (case lang
                   :clj 'clojure.test/deftest
                   :cljs 'cljs.test/deftest)
           :row row
           :col col
           :base-lang base-lang
           :lang lang
           :expr expr
           :arity arg-count}
          (analyze-defn ctx
                        (update expr :children
                                (fn [[_ name-expr & body]]
                                  (list*
                                   (token/token-node 'clojure.core/defn)
                                   name-expr
                                   (seq/vector-node [])
                                   body)))))))

(defn cons* [x xs]
  (if x (cons x xs)
      xs))

(defn analyze-binding-call [{:keys [:callstack] :as ctx} fn-name expr]
  (namespace/reg-used-binding! (:base-lang ctx)
                               (:lang ctx)
                               (-> ctx :ns :name)
                               (get (:bindings ctx) fn-name))
  (when-not (config/skip? :invalid-arity callstack)
    (let [filename (:filename ctx)
          children (:children expr)]
      (when-not (:call-as-use ctx)
        (when-let [{:keys [:fixed-arities :var-args-min-arity]}
                   (get (:arities ctx) fn-name)]
          (let [arg-count (count (rest children))]
            (when-not (or (contains? fixed-arities arg-count)
                          (and var-args-min-arity (>= arg-count var-args-min-arity)))
              (state/reg-finding! (node->line filename expr :error
                                              :invalid-arity
                                              (format "wrong number of args (%s) passed to %s"
                                                      arg-count
                                                      fn-name)))))))
      (analyze-children ctx (rest children)))))

(defn analyze-call
  [{:keys [:filename :fn-body :base-lang :lang :ns] :as ctx}
   {:keys [:arg-count
           :full-fn-name
           :row :col
           :expr]}]
  (let [children (:children expr)
        {resolved-namespace :ns
         resolved-name :name}
        (resolve-name
         (namespace/get-namespace base-lang lang (:name ns)) full-fn-name)
        [resolved-as-namespace resolved-as-name lint-as?]
        (or (when-let [[ns n] (config/lint-as [resolved-namespace resolved-name])]
              [ns n true])
            [resolved-namespace resolved-name false])
        fq-sym (when (and resolved-namespace
                          resolved-name)
                 (symbol (str resolved-namespace)
                         (str resolved-name)))
        ctx (if fq-sym
              (update ctx :callstack
                      (fn [cs]
                        (cons [resolved-namespace resolved-name] cs)))
              ctx)
        resolved-as-clojure-var-name
        (when (one-of resolved-as-namespace [clojure.core cljs.core])
          resolved-as-name)
        use (when lint-as?
              {:type :use
               :ns resolved-namespace
               :name resolved-name
               :row row
               :col col
               :base-lang base-lang
               :lang lang
               :expr expr})]
    (cons* use
           (case resolved-as-clojure-var-name
             ns
             (let [ns (analyze-ns-decl ctx expr)]
               [ns])
             in-ns (when-not fn-body [(analyze-in-ns ctx expr)])
             alias
             [(analyze-alias ctx expr)]
             (defn defn- defmacro)
             (cons {:type :call
                    :name resolved-as-clojure-var-name
                    :row row
                    :col col
                    :base-lang base-lang
                    :lang lang
                    :expr expr
                    :arity arg-count}
                   (analyze-defn ctx (lift-meta ctx expr)))
             comment
             (analyze-children ctx children)
             (-> some->)
             (analyze-expression** ctx (macroexpand/expand-> ctx expr))
             (->> some->>)
             (analyze-expression** ctx (macroexpand/expand->> ctx expr))
             (cond-> cond->> . .. deftype
                     proxy extend-protocol doto reify definterface defrecord defprotocol
                     defcurried)
             ;; don't lint calls in these expressions, only register them as used vars
             (analyze-children (assoc ctx :call-as-use true)
                               (:children expr))
             (let let* for doseq with-open)
             (analyze-like-let ctx expr)
             letfn
             (analyze-letfn ctx expr)
             (if-let when-let)
             (analyze-if-let ctx expr)
             do
             (analyze-do ctx expr)
             (fn fn*)
             (analyze-fn ctx (lift-meta ctx expr))
             case
             (analyze-case ctx expr)
             loop
             (analyze-loop ctx expr)
             recur
             (analyze-recur ctx expr)
             ;; catch-all
             (case [resolved-namespace resolved-name]
               [schema.core defn]
               (analyze-schema-defn ctx expr)
               ([clojure.test deftest] [cljs.test deftest])
               (analyze-deftest ctx resolved-namespace expr)
               ;; catch-all
               (let [call (if (:call-as-use ctx)
                            {:type :use
                             :ns resolved-namespace
                             :name resolved-name
                             :row row
                             :col col
                             :base-lang base-lang
                             :lang lang
                             :expr expr}
                            {:type :call
                             :name full-fn-name
                             :arity arg-count
                             :row row
                             :col col
                             :base-lang base-lang
                             :lang lang
                             :expr expr
                             :callstack (:callstack ctx)})
                     next-ctx (cond-> ctx
                                (= '[clojure.core.async thread]
                                   [resolved-namespace resolved-name])
                                (assoc-in [:recur-arity :fixed-arity] 0))]
                 (cons call (analyze-children next-ctx (rest children)))))))))

(defn lint-keyword-call! [{:keys [:callstack] :as ctx} kw namespaced? arg-count expr]
  (when-not (config/skip? :invalid-arity callstack)
    (let [ns (:ns ctx)
          ?resolved-ns (if namespaced?
                         (if-let [kw-ns (namespace kw)]
                           (or (get (:qualify-ns ns) (symbol kw-ns))
                               ;; because we couldn't resolve the namespaced
                               ;; keyword, we print it as is
                               (str ":" (namespace kw)))
                           ;; if the keyword is namespace, but there is no
                           ;; namespace, it's the current ns
                           (:name ns))
                         (namespace kw))
          kw-str (if ?resolved-ns (str ?resolved-ns "/" (name kw))
                     (str (name kw)))]
      (when (or (zero? arg-count)
                (> arg-count 2))
        (state/reg-finding! (node->line (:filename ctx) expr :error :invalid-arity
                                        (format "wrong number of args (%s) passed to keyword :%s"
                                                arg-count
                                                kw-str)))))))

(defn lint-map-call! [{:keys [:callstack] :as ctx} _the-map arg-count expr]
  (when-not (config/skip? :invalid-arity callstack)
    (when (or (zero? arg-count)
              (> arg-count 2))
      (state/reg-finding! (node->line (:filename ctx) expr :error :invalid-arity
                                      (format "wrong number of args (%s) passed to a map"
                                              arg-count))))))

(defn lint-symbol-call! [{:keys [:callstack] :as ctx} _the-symbol arg-count expr]
  (when-not (config/skip? :invalid-arity callstack)
    (when (or (zero? arg-count)
              (> arg-count 2))
      (state/reg-finding! (node->line (:filename ctx) expr :error :invalid-arity
                                      (format "wrong number of args (%s) passed to a symbol"
                                              arg-count))))))

(defn reg-not-a-function! [{:keys [:filename :callstack]} expr type]
  (when-not (config/skip? :not-a-function callstack)
    (state/reg-finding!
     (node->line filename expr :error :not-a-function (str "a " type " is not a function")))))

(defn analyze-expression**
  [{:keys [:bindings] :as ctx}
   {:keys [:children] :as expr}]
  (let [t (node/tag expr)
        {:keys [:row :col]} (meta expr)
        arg-count (count (rest children))]
    (case t
      :quote nil
      :syntax-quote (namespace/used-namespaces ctx true expr)
      :namespaced-map (analyze-namespaced-map (update ctx
                                                      :callstack #(cons [nil t] %))
                                              expr)
      :map (do (key-linter/lint-map-keys ctx expr)
               (analyze-children (update ctx
                                         :callstack #(cons [nil t] %)) children))
      :set (do (key-linter/lint-set ctx expr)
               (analyze-children (update ctx
                                         :callstack #(cons [nil t] %))
                                 children))
      :fn (recur ctx (macroexpand/expand-fn expr))
      :token (namespace/used-namespaces ctx false expr)
      :list
      (when-let [function (first children)]
        (let [t (node/tag function)]
          (case t
            :map
            (do (lint-map-call! ctx function arg-count expr)
                (analyze-children ctx children))
            :quote
            (let [quoted-child (-> function :children first)]
              (if (utils/symbol-token? quoted-child)
                (do (lint-symbol-call! ctx quoted-child arg-count expr)
                    (analyze-children ctx children))
                (analyze-children ctx children)))
            :token
            (if-let [k (:k function)]
              (do (lint-keyword-call! ctx k (:namespaced? function) arg-count expr)
                  (analyze-children ctx children))
              (if-let [full-fn-name (when (utils/symbol-token? function) (:value function))]
                (let [unqualified? (nil? (namespace full-fn-name))
                      binding-call? (and unqualified? (contains? bindings full-fn-name))]
                  (if binding-call?
                    (analyze-binding-call ctx full-fn-name expr)
                    (analyze-call ctx {:arg-count arg-count
                                       :full-fn-name full-fn-name
                                       :row row
                                       :col col
                                       :expr expr})))
                (cond
                  (utils/boolean-token? function)
                  (do (reg-not-a-function! ctx expr "boolean")
                      (analyze-children ctx (rest children)))
                  (utils/string-token? function)
                  (do (reg-not-a-function! ctx expr "string")
                      (analyze-children ctx (rest children)))
                  (utils/char-token? function)
                  (do (reg-not-a-function! ctx expr "character")
                      (analyze-children ctx (rest children)))
                  (utils/number-token? function)
                  (do (reg-not-a-function! ctx expr "number")
                      (analyze-children ctx (rest children)))
                  :else
                  (analyze-children ctx children))))
            (analyze-children ctx children))))
      ;; catch-all
      (analyze-children (update ctx
                                :callstack #(cons [nil t] %))
                        children))))

(comment
  (parse-string "^{:key a} []")
  )

(defn analyze-expression*
  [{:keys [:filename :base-lang :lang :results :ns :expression :debug?]}]
  (let [ctx {:filename filename
             :base-lang base-lang
             :lang lang
             :ns ns
             :bindings {}}]
    (loop [ns ns
           [first-parsed & rest-parsed :as all] (analyze-expression** ctx expression)
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
               (update :used into (:used first-parsed))
               (update :required into (:required first-parsed))))
          :use
          (do
            (namespace/reg-usage! base-lang lang (:name ns) (:ns first-parsed))
            (recur
             ns
             rest-parsed
             (-> results
                 (update :used conj (:ns first-parsed)))))
          (:duplicate-map-key
           :missing-map-value
           :duplicate-set-key
           :invalid-bindings
           :invalid-arity)
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
             (let [;; TODO: can we do without this resolve since we already resolved in analyze-expression**?
                   resolved (resolve-name
                             (namespace/get-namespace base-lang lang (:name ns)) (:name first-parsed))
                   first-parsed (assoc first-parsed
                                       :name (:name resolved)
                                       :ns (:name ns))]
               (case (:type first-parsed)
                 :defn
                 (let [;; _ (println "LANG FP" (name (:lang first-parsed)))
                       path (if (= :cljc base-lang)
                              [:defs (:name ns) (:lang first-parsed) (:name resolved)]
                              [:defs (:name ns) (:name resolved)])
                       results
                       (if resolved
                         (do
                           (namespace/reg-var! ctx (:name ns) (:name resolved) (:expr first-parsed))
                           (assoc-in results path
                                     (dissoc first-parsed
                                             :type
                                             :expr)))
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
                         results (do
                                   (when-not unqualified?
                                     (namespace/reg-usage! base-lang lang (:name ns)
                                                           (:ns resolved)))
                                   (cond-> (update-in results path vconj call)
                                     (not unqualified?)
                                     (update :used conj (:ns resolved))))]
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
        [ns results]))))

(defn analyze-expressions
  "Analyzes expressions and collects defs and calls into a map. To
  optimize cache lookups later on, calls are indexed by the namespace
  they call to, not the ns where the call occurred. Also collects
  other findings and passes them under the :findings key."
  [{:keys [:filename :base-lang :lang :expressions :debug?]}]
  (profiler/profile
   :analyze-expressions
   (loop [ns (analyze-ns-decl {:filename filename
                               :base-lang base-lang
                               :lang lang} (parse-string "(ns user)"))
          [expression & rest-expressions] expressions
          results {:calls {}
                   :defs {}
                   :required (:required ns)
                   :used (:used ns)
                   :findings []
                   :lang base-lang}]
     (if expression
       (let [[ns results]
             (analyze-expression* {:filename filename
                                   :base-lang base-lang
                                   :lang lang
                                   :ns ns
                                   :results results
                                   :expression expression
                                   :debug? debug?})]
         (recur ns rest-expressions results))
       results))))

;;;; processing of string input

(defn analyze-input
  "Analyzes input and returns analyzed defs, calls. Also invokes some
  linters and returns their findings."
  [filename input lang dev?]
  (try
    (let [parsed (p/parse-string input)
          analyzed-expressions
          (if (= :cljc lang)
            (let [clj (analyze-expressions {:filename filename
                                            :base-lang :cljc
                                            :lang :clj
                                            :expressions (:children (select-lang parsed :clj))})
                  cljs (analyze-expressions {:filename filename
                                             :base-lang :cljc
                                             :lang :cljs
                                             :expressions (:children (select-lang parsed :cljs))})]
              (profiler/profile :deep-merge
                                (deep-merge clj cljs)))
            (analyze-expressions {:filename filename
                                  :base-lang lang
                                  :lang lang
                                  :expressions
                                  (:children parsed)}))]
      analyzed-expressions)
    (catch Exception e
      (if dev? (throw e)
          {:findings [{:level :error
                       :filename filename
                       :col 0
                       :row 0
                       :message (str "can't parse "
                                     filename ", "
                                     (.getMessage e))}]}))
    (finally
      (when (-> @config/config :output :show-progress)
        (print ".") (flush)))))
