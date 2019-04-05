(ns clj-kondo.impl.vars
  {:no-doc true}
  (:require
   [clj-kondo.impl.utils :refer [some-call node->line
                                 parse-string parse-string-all]]
   [clojure.set :as set]
   [rewrite-clj.node.protocols :as node]
   [clojure.string :as str]))

;;;; function arity

(defn arg-name [{:keys [:children] :as expr}]
  ;; TODO: use strip-meta
  (if-let [n (:value expr)]
    ;; normal argument
    n
    ;; this is an argument with metadata
    (-> children last :value)))

(defn analyze-arity [{:keys [:children] :as arg-decl}]
  (loop [args children
         arity 0
         ;; max-arity nil
         ;; varargs? false
         arg-names #{}]
    (if-let [arg (first args)]
      (if (= '& (:value arg))
        {:arg-names arg-names
         :min-arity arity
         :varargs? true}
        (recur (rest args)
               (inc arity)
               ;; varargs?
               (conj arg-names (arg-name arg))))
      {:arg-names arg-names
       :fixed-arity arity})))

(defn require-clause? [{:keys [:children] :as expr}]
  (= :require (:k (first children))))

(defn refer-clojure-clause? [{:keys [:children] :as expr}]
  (= :refer-clojure (:k (first children))))

(defn analyze-require-subclause [{:keys [:children] :as expr}]
  (when (= :vector (:tag expr))
    ;; ns-name can be a string in CLJS projects, that's why we can't just take the :value
    ;; see #51
    (let [ns-name (symbol (node/sexpr (first children)))]
      (loop [children (rest children)
             as nil
             refers []]
        (if-let [child (first children)]
          (cond (= :refer (:k child))
                (recur
                 (nnext children)
                 as
                 (into refers (map :value (:children (fnext children)))))
                (= :as (:k child))
                (recur
                 (nnext children)
                 (:value (fnext children))
                 refers))
          {:type :require
           :ns ns-name
           :as as
           :refers (map (fn [refer]
                          [refer {:namespace ns-name
                                  :name (symbol (str ns-name) (str refer))}])
                        refers)})))))

(defn analyze-ns-decl [{:keys [:children] :as expr}]
  ;; TODO: analyze refer clojure exclude
  ;; TODO: just apply strip-meta and remove handling of meta here
  (let [requires (filter require-clause? children)
        subclauses (mapcat #(map analyze-require-subclause
                                 (rest (:children %)))
                           requires)]
    {:type :ns
     :name (symbol (let [name-node (second children)]
                     (if (contains? #{:meta :meta*} (node/tag name-node))
                       (:value (last (:children name-node)))
                       (:value name-node))))
     :qualify-var (into {} (mapcat #(mapcat (comp :refers analyze-require-subclause)
                                            (:children %)) requires))
     :qualify-ns (reduce (fn [acc sc]
                           (cond-> (assoc acc (:ns sc) (:ns sc))
                             (:as sc)
                             (assoc (:as sc) (:ns sc))))
                         {}
                         subclauses)
     :clojure-excluded (set (for [c children
                                  :when (refer-clojure-clause? c)
                                  [k v] (partition 2 (rest (:children c)))
                                  :when (= :exclude (:k k))
                                  sym (:children v)]
                              (:value sym)))}))

(comment

  ;; TODO: turn this into test
  (analyze-ns-decl (parse-string "(ns foo (:refer-clojure :exclude [get nth]) (:refer-clojure :exclude [time assoc]))"))
  )

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
      (if (contains? '#{:meta} (node/tag child))
        (recur rest-children
               (into stripped (strip-meta* (rest (:children child)))))
        (recur rest-children
               (conj stripped child)))
      stripped)))

(defn strip-meta [expr]
  (assoc expr
         :children (strip-meta* (:children expr))))

(declare parse-arities)

(defn parse-defn [lang bindings expr]
  (let [children (:children (strip-meta expr))
        ;; TODO: add metadata parsing for private
        private? (= 'defn- (some-call expr defn-))
        children (rest children)
        fn-name (:value (first (filter #(symbol? (:value %)) children)))
        arg-decl (first (filter #(= :vector (:tag %)) children))
        arg-decls (map (fn [x]
                         ;; skip docstring, etc.
                         (first
                          (keep
                           #(cond (= :vector (:tag %))
                                  %
                                  (= :meta (:tag %))
                                  (last (:children %)))
                           (:children x))))
                       (filter #(= :list (:tag %)) (rest children)))
        arg-decls (if arg-decl [arg-decl]
                      arg-decls)
        arities (map analyze-arity arg-decls)
        fixed-arities (set (keep :fixed-arity arities))
        var-args-min-arity (:min-arity (first (filter :varargs? arities)))
        defn
        (let [{:keys [:row :col]} (meta expr)]
          (if fn-name
            (cond-> {:type :defn
                     :name fn-name
                     :row row
                     :col col
                     :lang lang}
              (seq fixed-arities) (assoc :fixed-arities fixed-arities)
              private? (assoc :private? private?)
              var-args-min-arity (assoc :var-args-min-arity var-args-min-arity))
            {:type :debug
             :level :info
             :message "Could not parse defn form"
             :row row
             :col col
             :lang lang}))]
    (cons defn
          (mapcat
           #(parse-arities lang (reduce set/union bindings
                                        (map :arg-names arities))
                           %)
           (rest children)))))

(defn reader-conditional-expr? [expr]
  (and (= :reader-macro (node/tag expr))
       (= '? (:value (first (:children expr))))))

(defn parse-reader-conditional [expr]
  (let [kvs (-> expr
                :children second :children)
        kvs (partition 2 kvs)]
    (into {}
          (for [[k v] kvs]
            [(:k k) v]))))

(defn parse-arities
  ;; TODO: refactor and split into multiple functions
  ([lang expr] (parse-arities lang #{} expr))
  ([lang bindings {:keys [:children] :as expr}]
   (cond
     (some-call expr ns)
     [(analyze-ns-decl expr)]
     ;; TODO: in-ns is not supported yet
     ;; One thing to note: if in-ns is used in a function body, the rest of the namespace is now analyzed in that namespace, which is incorrect.
     ;; (some-call expr in-ns)
     ;; [(analyze-in-ns expr)]
     ;; TODO: better resolving for these macro calls
     ;; core/->> was hard-coded here because of
     ;; https://github.com/clojure/clojurescript/blob/73272a2da45a4c69d090800fa7732abe3fd05c70/src/main/clojure/cljs/core.cljc#L551
     (some-call expr defn defn- core/defn core/defn- defmacro)
     (parse-defn lang bindings expr)
     ;; TODO: better resolving for these macro calls
     ;; core/->> was hard-coded here because of
     ;; https://github.com/clojure/clojurescript/blob/73272a2da45a4c69d090800fa7732abe3fd05c70/src/main/clojure/cljs/core.cljc#L854
     (or (some-call expr core/->> ->> cond-> cond->> some-> some->> . .. deftype
                    proxy extend-protocol doto reify definterface defrecord defprotocol)
         (contains? '#{:quote :syntax-quote} (node/tag expr)))
     []
     (some-call expr let)
     (let [let-bindings (->> children second :children (map :value) (filter symbol?) set)]
       (mapcat #(parse-arities lang (set/union bindings let-bindings) %) (rest children)))
     (some-call expr fn)
     ;; TODO better arity analysis like in normal fn
     (let [fn-name (-> children second :value)
           arg-vec (first (filter #(= :vector (node/tag %)) (rest children)))
           maybe-bindings (->> arg-vec :children (map :value))
           fn-bindings (set (filter symbol? (cons fn-name maybe-bindings)))]
       (mapcat #(parse-arities lang (set/union bindings fn-bindings) %) (rest children)))
     (fn-call? expr)
     (let [fn-name (:value (first children))
           args (count (rest children))
           binding-call? (contains? bindings fn-name)
           parse-rest (mapcat #(parse-arities lang bindings %) (rest children))]
       (if binding-call?
         parse-rest
         (cons
          (let [{:keys [:row :col]} (meta expr)]
            {:type :call
             :name fn-name
             :arity args
             :row row
             :col col
             :lang lang})
          parse-rest)))
     (reader-conditional-expr? expr)
     (let [{:keys [:clj :cljs]} (parse-reader-conditional expr)]
       (for [[l e] [[:clj clj] [:cljs cljs]]
             :when e
             expr (parse-arities l bindings e)]
         expr))
     :else (mapcat #(parse-arities lang bindings %) children))))

(comment
  (parse-string "`(foo)")
  )

(defn qualify-name [ns nm]
  (if-let [ns* (namespace nm)]
    (when-let [ns* (get (:qualify-ns ns) (symbol ns*))]
      {:namespace ns*
       :name (symbol (str ns*)
                     (name nm))})
    (or (get (:qualify-var ns)
             nm)
        (let [namespace (or (:name ns) 'user)]
          {:namespace namespace
           :name (symbol (str namespace) (str nm))
           :clojure-excluded? (contains? (:clojure-excluded ns)
                                         nm)}))))

(def vconj (fnil conj []))

(defn analyze-arities
  "Collects defns and calls into a map. To optimize cache lookups later
  on, calls are indexed by the namespace they call to, not the
  ns where the call occurred."
  ([filename lang expr] (analyze-arities filename lang expr false))
  ([filename lang expr debug?]
   (loop [[first-parsed & rest-parsed] (parse-arities lang expr)
          ns {:name 'user}
          results {:calls {}
                   :defns {}
                   :findings []
                   :lang lang}]
     (if first-parsed
       (case (:type first-parsed)
         (:ns :in-ns)
         (recur rest-parsed
                first-parsed
                results)
         (recur rest-parsed
                ns
                (case (:type first-parsed)
                  :debug
                  (update-in results
                             [:findings]
                             conj
                             (assoc first-parsed
                                    :filename filename))
                  (let [qname (qualify-name ns (:name first-parsed))
                        first-parsed (cond->
                                         (assoc first-parsed
                                                :qname (:name qname)
                                                :ns (:name ns)
                                                :filename filename
                                                )
                                       (not= lang (:lang first-parsed))
                                       (assoc :base-lang lang))]
                    (case (:type first-parsed)
                      :defn
                      (let [path (case lang
                                   :cljc [:defns (:name ns) (:lang first-parsed) (:name qname)]
                                   [:defns (:name ns) (:name qname)])
                            results
                            (if qname
                              (assoc-in results path
                                        first-parsed)
                              ;; what's this for?
                              (update-in results
                                         [:findings]
                                         vconj
                                         first-parsed))]
                        (if debug?
                          (update-in results
                                     [:findings]
                                     vconj
                                     (assoc first-parsed
                                            :level :info
                                            :message
                                            (str/join " "
                                                      ["Defn resolved as"
                                                       (:name qname) "with arities"
                                                       "fixed:"(:fixed-arities first-parsed)
                                                       "varargs:"(:var-args-min-arity first-parsed)])
                                            :type :debug))
                          results))
                      :call
                      (if qname
                        (let [path [:calls (:namespace qname)]
                              call (cond-> first-parsed
                                     (:clojure-excluded? qname)
                                     (assoc :clojure-excluded? true))
                              results (update-in results path vconj call)]
                          (if debug? (update-in results [:findings] conj
                                                (assoc call
                                                       :level :info
                                                       :message (str "Call resolved as "
                                                                     (:name qname))
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
                          results)))))))
       [results]))))

(defn core-lookup
  [clojure-core-defns cljs-core-defns lang var-name]
  #_(println (keys clojure-core-defns))
  #_(println (keys cljs-core-defns))
  (let [core (case lang
               :clj clojure-core-defns
               :cljs cljs-core-defns
               :cljc clojure-core-defns)
        sym (case lang
              :clj (symbol "clojure.core"
                           (name var-name))
              :cljs (symbol "cljs.core"
                            (name var-name))
              :cljc (symbol "clojure.core"
                            (name var-name)))]
    (get core sym)))

(defn fn-call-findings
  "Analyzes indexed defns and calls and returns incorrect function
  calls."
  [idacs]
  (let [clojure-core-defns (get-in idacs [:clj :defns 'clojure.core])
        cljs-core-defns (get-in idacs [:cljs :defns 'cljs.core])
        findings (for [lang [:clj :cljs :cljc]
                       ns-sym (keys (get-in idacs [lang :calls]))
                       call (get-in idacs [lang :calls ns-sym])
                       :let [fn-name (:qname call)
                             caller-ns (:ns call)
                             fn-ns (symbol (namespace fn-name))
                             called-fn
                             (or (get-in idacs [lang :defns fn-ns fn-name])
                                 (get-in idacs [:cljc :defns fn-ns :cljc fn-name])
                                 (get-in idacs [:cljc :defns fn-ns (:lang call) fn-name])
                                 (when (and
                                        (not (:clojure-excluded? call))
                                        (= (:ns call)
                                           fn-ns))
                                   (core-lookup clojure-core-defns cljs-core-defns
                                                lang fn-name)))
                             ;; update fn-ns in case it's resolved as a clojure core function
                             fn-ns (:ns called-fn)]
                       :when called-fn
                       :let [;; _ (println "call" call "called-fn" called-fn)
                             ;; a macro in a CLJC file with the same namespace
                             ;; in that case, looking at the row and column is
                             ;; not reliable.  we may look at the lang of the
                             ;; call and the lang of the function def context in
                             ;; the case of in-ns, the bets are off. we may
                             ;; support in-ns in a next version.
                             valid-order? (if (and (= (:ns call)
                                                      fn-ns)
                                                   (= (:base-lang call)
                                                      (:base-lang called-fn)))
                                            (or (> (:row call) (:row called-fn))
                                                (and (= (:row call) (:row called-fn))
                                                     (> (:col call) (:col called-fn))))
                                            true)]
                       :when valid-order?
                       :let [arity (:arity call)
                             filename (:filename call)
                             fixed-arities (:fixed-arities called-fn)
                             var-args-min-arity (:var-args-min-arity called-fn)
                             errors
                             [(when-not
                                  (or (contains? fixed-arities arity)
                                      (and var-args-min-arity (>= arity var-args-min-arity)))
                                {:filename filename
                                 :row (:row call)
                                 :col (:col call)
                                 :level :error
                                 :type :invalid-arity
                                 :message (format "Wrong number of args (%s) passed to %s"
                                                  (str (:arity call) #_#_" " called-fn)
                                                  (:qname called-fn))})
                              (when (and (:private? called-fn)
                                         (not= (:ns call)
                                               fn-ns))
                                {:filename filename
                                 :row (:row call)
                                 :col (:col call)
                                 :level :error
                                 :type :private-call
                                 :message (format "Call to private function %s"
                                                  (:name call))})]]
                       e errors
                       :when e]
                   e)]
    findings))

;;;; Scratch

(comment
  )
