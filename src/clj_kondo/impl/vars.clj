;; NOTE: this namespace is a bit messy. The code should be cleaned up before release.

(ns clj-kondo.impl.vars
  {:no-doc true}
  (:require
   [clojure.set :as set]
   [clj-kondo.impl.utils :refer [call? node->line]]
   [rewrite-clj.node.protocols :as node]))

;;;; function arity

(defn arg-name [{:keys [:children] :as rw-expr}]
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

(comment
  (analyze-arity (parse-string "[x y z]")))

(defn function-def? [rw-expr]
  (call? rw-expr 'defn 'defn-))

(defn let? [rw-expr]
  (call? rw-expr 'let))

(defn anon-fn? [rw-expr]
  (call? rw-expr 'fn))

(defn ns-decl? [rw-expr]
  (call? rw-expr 'ns))

(defn require-clause? [{:keys [:children] :as rw-expr}]
  (= :require (:k (first children))))

(defn analyze-require-subclause [{:keys [:children] :as rw-expr}]
  (if (= :vector (:tag rw-expr))
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
           :as (when as [as ns-name])
           :refers (map (fn [refer]
                          [refer (symbol (str ns-name) (str refer))])
                        refers)})))))

(defn analyze-ns-decl [{:keys [:children] :as rw-expr}]
  ;; TODO: analyze refer clojure exclude
  {:type :ns
   :name (let [name-node (second children)]
           (if (contains? #{:meta :meta*} (node/tag name-node))
             (:value (last (:children name-node)))
             (:value name-node)))
   :qualify-var (let [requires (filter require-clause? children)]
                  (into {} (mapcat #(mapcat (comp :refers  analyze-require-subclause) (:children %)) requires)))
   :qualify-ns (let [requires (filter require-clause? children)]
                 (into {} (mapcat #(map (comp :as analyze-require-subclause) (:children %)) requires)))})

(comment
  (analyze-ns-decl (parse-string "(ns (:require [foo :as bar :refer [baz]]))"))
  (analyze-ns-decl (parse-string "(ns #^{} clojure.math)"))
  (select-keys)
  )

(defn function-call? [rw-expr]
  (and (= :list (:tag rw-expr))
       (symbol? (:value (first (:children rw-expr))))))

(defn strip-meta [{:keys [:children] :as rw-expr}]
  (loop [children children
         stripped []]
    (if-let [child (first children)]
      (if (contains? '#{:meta} (node/tag child))
        (recur (rest children)
               (into stripped (rest (:children child))))
        (recur (rest children)
               (conj stripped child)))
      (assoc rw-expr :children stripped))))

(comment
  (strip-meta (parse-string "[^String x]")))

(defn parse-arities
  ([rw-expr] (parse-arities rw-expr #{}))
  ([{:keys [:children] :as rw-expr} bindings]
   (let [fdef? (function-def? rw-expr)]
     (cond
       (ns-decl? rw-expr)
       [(analyze-ns-decl rw-expr)]
       fdef?
       (let [children (:children (strip-meta rw-expr))
             private? (= fdef? 'defn-)
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
             var-args-min-arity (:min-arity (first (filter :varargs? arities)))]
         (reduce into [(cond-> {:type :defn
                                :name fn-name
                                ;; :arities arities
                                }
                         (seq fixed-arities) (assoc :fixed-arities fixed-arities)
                         private? (assoc :private? private?)
                         var-args-min-arity (assoc :var-args-min-arity var-args-min-arity))]
                 (map #(parse-arities % (reduce set/union bindings (map :arg-names arities))) (rest children))))
       (call? rw-expr '->> 'cond-> 'cond->> 'some-> 'some->> '. '.. 'deftype
              'proxy 'extend-protocol 'doto 'reify)
       []
       (let? rw-expr)
       (let [let-bindings (->> children second :children (map :value) (filter symbol?) set)]
         (mapcat #(parse-arities % (set/union bindings let-bindings)) (rest children)))
       (anon-fn? rw-expr)
       ;; TODO better arity analysis like in normal fn
       (let [fn-name (-> children second :value)
             arg-vec (first (filter #(= :vector (node/tag %)) (rest children)))
             ;;_ (println "arg vec" arg-vec)
             maybe-bindings (->> arg-vec :children (map :value))
             fn-bindings (set (filter symbol? (cons fn-name maybe-bindings)))]
         (mapcat #(parse-arities % (set/union bindings fn-bindings)) (rest children)))
       (function-call? rw-expr)
       (let [fn-name (:value (first children))
             args (count (rest children))
             binding-call? (contains? bindings fn-name)]
         (reduce into (if binding-call?
                        []
                        [{:type :call
                          :name fn-name
                          :arity args
                          :node rw-expr
                          ;; TODO: add namespace in which function is called
                          }])
                 (map #(parse-arities % bindings) (rest children))))
       :else (mapcat #(parse-arities % bindings) children)))))

(comment
  (parse-arities (parse-string "(defn foo \"docstring\" ([stackstrace sms]) ([stacktrace sms opts]))"))
  (parse-arities (parse-string "(defn str \"docstring\" {:tag String :added 1.0} (^String [stackstrace sms]) ([stacktrace sms opts]))"))
  (parse-arities (parse-string "(defn class \"docstring\" {:some-map 1} ^Class [^Object x])"))

  (defn class
    "Returns the Class of x"
    {:added "1.0"
     :static true}
    ^Class [^Object x] (if (nil? x) x (. x (getClass))))

  ;; TODO: (assoc) doesn't give a warning
  (assoc)
  (assoc-in {})
  )


(defn qualify-name [ns nm]
  ;; when = workaround for bad defn parsing
  (when nm
    (if-let [ns* (namespace nm)]
      (let [ns* (or (get (:qualify-ns ns) (symbol ns*))
                    ns*)]
        (symbol (str ns*)
                (name nm)))
      (or (get (:qualify-var ns)
               nm)
          #_(or (when (contains? public-clj-fns nm)
                  (symbol "clojure.core"
                          (str nm))))
          (symbol (str (or (:name ns) "user"))
                  (str nm))))))

(defn analyze-arities [filename language rw-expr]
  (loop [parsed (parse-arities rw-expr)
         ns {:name 'user}
         qualified {:calls []
                    :defns {}}]
    (if-let [p (first parsed)]
      (do
        ;; (println (:type p))
        (if (= :ns (:type p))
          (recur (rest parsed)
                 p
                 qualified)
          (recur (rest parsed)
                 ns
                 (case (:type p)
                   :defn
                   (let [qname (qualify-name ns (:name p))]
                     (assoc-in qualified [:defns (:name ns) qname]
                               (assoc p
                                      :name qname
                                      :ns (:name ns)
                                      :filename filename
                                      :lang language)))
                   :call
                   (let [qname (qualify-name ns (:name p))]
                     (update qualified :calls
                             conj (assoc p
                                         :name qname
                                         :caller-ns (:name ns)
                                         :filename filename
                                         :lang language)))))))
      qualified)))

(defn arity-findings
  [clj-defns cljs-defns calls]
  (let [clojure-core-defns (get clj-defns 'clojure.core)
        cljs-core-defns (get cljs-defns 'cljs.core)
        findings (for [call calls
                       :let [fn-name (:name call)
                             fn-ns (symbol (namespace fn-name))
                             arity (:arity call)
                             filename (:filename call)
                             language (:lang call)
                             dict (case language
                                    :clj clj-defns
                                    :cljs cljs-defns)
                             called-fn (or (get-in dict [fn-ns fn-name])
                                           (when (= (:caller-ns call)
                                                    fn-ns)
                                             ;; we resolved the call as
                                             ;; ns-local, but it might be a
                                             ;; clojure.core call
                                             (case language
                                               :clj
                                               (get clojure-core-defns
                                                    (symbol "clojure.core"
                                                            (name fn-name)))
                                               :cljs
                                               (get cljs-core-defns
                                                    (symbol "cljs.core"
                                                            (name fn-name))))))
                             fixed-arities (:fixed-arities called-fn)
                             var-args-min-arity (:var-args-min-arity called-fn)]
                       :when called-fn
                       :let [errors
                             [(when-not (or (contains? fixed-arities arity)
                                            (and var-args-min-arity (>= arity var-args-min-arity)))
                                (node->line filename
                                            (:node call)
                                            :error
                                            :invalid-arity
                                            (format "Wrong number of args (%s) passed to %s"
                                                    (:arity call)
                                                    (:name called-fn))))
                              (when (and (:private? called-fn)
                                         (not= (:caller-ns call)
                                               fn-ns))
                                (node->line filename (:node call) :error :private-call
                                            (format "Call to private function %s"
                                                    (:name call))))]]
                       e errors
                       :when e]
                   e)]
    findings))

;;;; Scratch

(comment
  )
