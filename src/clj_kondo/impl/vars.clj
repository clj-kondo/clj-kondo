(ns clj-kondo.impl.vars
  {:no-doc true}
  (:require
   [clj-kondo.impl.utils :refer [some-call node->line
                                 parse-string parse-string-all]]
   [clojure.set :as set]
   [rewrite-clj.node.protocols :as node]))

;;;; function arity

(defn arg-name [{:keys [:children] :as rw-expr}]
  ;; TODO: use strip-meta
  (if-let [n (:value rw-expr)]
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

(defn defn? [rw-expr]
  (some-call rw-expr defn defn- defmacro))

(defn let? [rw-expr]
  (some-call rw-expr let))

(defn anon-fn? [rw-expr]
  (some-call rw-expr fn))

(defn ns-decl? [rw-expr]
  (some-call rw-expr ns))

(defn require-clause? [{:keys [:children] :as rw-expr}]
  (= :require (:k (first children))))

(defn analyze-require-subclause [{:keys [:children] :as expr}]
  (when (= :vector (:tag expr))
    (let [ns-name (:value (first children))]
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
                          [refer {:namespace (symbol ns-name)
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
                         subclauses)}))

(defn fn-call? [rw-expr]
  (let [tag (node/tag rw-expr)]
    (and (= :list tag)
         (symbol? (:value (first (:children rw-expr)))))))

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

(defn parse-defn [expr bindings]
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
                     :col col}
              (seq fixed-arities) (assoc :fixed-arities fixed-arities)
              private? (assoc :private? private?)
              var-args-min-arity (assoc :var-args-min-arity var-args-min-arity))
            {:type :debug
             :level :info
             :message "Could not parse defn form"
             :debug? true
             :row row
             :col col}))]
    (cons defn
          (mapcat
           #(parse-arities %
                           (reduce set/union bindings
                                   (map :arg-names arities)))
           (rest children)))))

(defn parse-arities
  ;; TODO: refactor and split into multiple functions
  ([expr] (parse-arities expr #{}))
  ([{:keys [:children] :as expr} bindings]
   (cond
     (ns-decl? expr)
     [(analyze-ns-decl expr)]
     (defn? expr)
     (parse-defn expr bindings)
     (some-call expr ->> cond-> cond->> some-> some->> . .. deftype
                proxy extend-protocol doto reify)
     []
     (let? expr)
     (let [let-bindings (->> children second :children (map :value) (filter symbol?) set)]
       (mapcat #(parse-arities % (set/union bindings let-bindings)) (rest children)))
     (anon-fn? expr)
     ;; TODO better arity analysis like in normal fn
     (let [fn-name (-> children second :value)
           arg-vec (first (filter #(= :vector (node/tag %)) (rest children)))
           maybe-bindings (->> arg-vec :children (map :value))
           fn-bindings (set (filter symbol? (cons fn-name maybe-bindings)))]
       (mapcat #(parse-arities % (set/union bindings fn-bindings)) (rest children)))
     (fn-call? expr)
     (let [fn-name (:value (first children))
           args (count (rest children))
           binding-call? (contains? bindings fn-name)
           parse-rest (mapcat #(parse-arities % bindings) (rest children))]
       (if binding-call?
         parse-rest
         (cons
          (let [{:keys [:row :col]} (meta expr)]
            {:type :call
             :name fn-name
             :arity args
             :row row
             :col col})
          parse-rest)))
     :else (mapcat #(parse-arities % bindings) children))))

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
           :name (symbol (str namespace) (str nm))}))))

(def vconj (fnil conj []))

(defn analyze-arities
  "Collects defns and calls into a map. To optimize cache lookups later
  on, calls are indexed by the namespace they call to, not the
  ns where the call occurred."
  [filename language expr]
  (loop [[first-parsed & rest-parsed] (parse-arities expr)
         ns {:name 'user}
         results {:calls {}
                  :defns {}
                  :findings []}]
    (if first-parsed
      (if (= :ns (:type first-parsed))
        (recur rest-parsed
               first-parsed
               results)
        (recur rest-parsed
               ns
               (if-not (contains? #{:defn :call} (:type first-parsed))
                 (update-in results
                            [:findings]
                            conj
                            first-parsed)
                 (let [qname (qualify-name ns (:name first-parsed))
                       first-parsed (assoc first-parsed
                                           :qname (:name qname)
                                           :ns (:name ns)
                                           :filename filename
                                           :lang language)]
                   (case (:type first-parsed)
                     :defn
                     (if qname
                       (assoc-in results [:defns (:name ns) (:name qname)]
                                 first-parsed)
                       (update-in results
                                  [:findings]
                                  vconj
                                  first-parsed))
                     :call
                     (if qname
                       (update-in results
                                  [:calls (:namespace qname)]
                                  vconj first-parsed)
                       (update-in results
                                  [:findings]
                                  conj
                                  (assoc first-parsed
                                         :level :info
                                         :message (str "Unrecognized call to "
                                                       (:name first-parsed))
                                         :type :unqualified-call
                                         :debug? true))))))))
      results)))

(defn core-lookup
  [clojure-core-defns cljs-core-defns lang var-name]
  (let [core (case lang :clj clojure-core-defns :cljs cljs-core-defns)
        sym (case lang
              :clj (symbol "clojure.core"
                           (name var-name))
              :cljs (symbol "cljs.core"
                            (name var-name)))]
    (get core sym)))

(defn fn-call-findings
  "Analyzes indexed defns and calls and returns incorrect function
  calls."
  [idacs]
  (let [clojure-core-defns (get-in idacs [:clj :defns 'clojure.core])
        cljs-core-defns (get-in idacs [:cljs :defns 'cljs.core])
        findings (for [lang [:clj :cljs]
                       ns-sym (keys (get-in idacs [lang :calls]))
                       call (get-in idacs [lang :calls ns-sym])
                       :let [fn-name (:qname call)
                             fn-ns (symbol (namespace fn-name))
                             called-fn (or (get-in idacs [lang :defns fn-ns fn-name])
                                           (when (= (:ns call)
                                                    fn-ns)
                                             ;; we resolved the call as
                                             ;; ns-local, but it might be a
                                             ;; clojure.core call
                                             (core-lookup clojure-core-defns cljs-core-defns
                                                          lang fn-name)))]
                       :when called-fn
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
                                                  (str (:arity call) " / " fixed-arities " / " called-fn)
                                                  (:name called-fn))})
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
