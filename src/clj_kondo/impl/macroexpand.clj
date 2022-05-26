(ns clj-kondo.impl.macroexpand
  {:no-doc true}
  (:require
   [clj-kondo.impl.utils :refer [parse-string tag vector-node list-node token-node]]
   [clojure.walk :as walk]))

(set! *warn-on-reflection* true)

(defn with-meta-of [x y]
  (let [m (meta y)
        m* (:meta y)
        x (if m* (assoc x :meta m*) x)]
    (with-meta x m)))

(defn expand-> [_ctx expr]
  (let [expr expr
        children (:children expr)
        [c & cforms] (rest children)
        ret (loop [x c, forms cforms]
              (if forms
                (let [form (first forms)
                      threaded (if (= :list (tag form))
                                 (with-meta-of
                                   (list-node (list* (first (:children form))
                                                     x
                                                     (next (:children form))))
                                   form)
                                 (with-meta-of (list-node (list form x))
                                   form))]
                  (recur threaded (next forms)))
                x))]
    ret))

(defn expand->> [_ctx expr]
  (let [expr expr
        children (:children expr)
        [c & cforms] (rest children)]
    (loop [x c, forms cforms]
      (if forms
        (let [form (first forms)
              threaded
              (if (= :list (tag form))
                (with-meta-of
                  (list-node
                   (concat
                    (cons (first (:children form))
                          (next (:children form)))
                    (list x)))
                  form)
                (with-meta-of (list-node (list form x))
                  form))]
          (recur threaded (next forms)))
        x))))

(defn expand-cond->
  "Expands cond-> and cond->>"
  [_ctx expr resolved-as-name]
  (let [[_ start-expr & clauses] (:children expr)
        thread-sym (case resolved-as-name
                     cond-> 'clojure.core/->
                     cond->> 'clojure.core/->>)
        g (with-meta-of (token-node (gensym))
            (with-meta start-expr (assoc (meta start-expr) :clj-kondo.impl/generated true)))
        steps (map (fn [[t step]]
                     (list-node [(token-node 'if)
                                 t
                                 (list-node
                                  [(token-node thread-sym)
                                   g
                                   step])
                                 g]))
                   (partition 2 clauses))
        ret (list-node [(token-node 'clojure.core/let)
                        (vector-node
                         (list* g start-expr
                                (interleave (repeat g) (butlast steps))))
                        (if (empty? steps) g (last steps))])]
    ret))

(defn expand-doto [_ctx expr]
  (let [[_doto x & forms] (:children expr)
        gx (with-meta-of (token-node (gensym "_"))
             (with-meta x (assoc (meta x) :clj-kondo.impl/generated true)))
        ret (list-node
             (list* (token-node 'clojure.core/let) (vector-node [gx x])
                    (map (fn [f]
                           (with-meta-of
                             (let [t (tag f)]
                               (if (= :list t)
                                 (let [fc (:children f)]
                                   (list-node (list* (first fc) gx (next fc))))
                                 (list-node [f gx])))
                             f)) forms)))]
    ret))

(defn expand-dot-constructor
  [_ctx expr]
  (let [[ctor-node & children] (:children expr)
        ctor (:value ctor-node)
        ctor-name (str ctor)
        ctor-name (-> ctor-name
                      (subs 0 (dec (count ctor-name)))
                      symbol)
        ctor-node (with-meta-of (token-node ctor-name)
                    ctor-node)]
    (with-meta-of (list-node (list* (token-node 'new) ctor-node children))
      expr)))

(defn expand-method-invocation
  [_ctx expr]
  (let [[meth-node invoked & args] (:children expr)
        meth (:value meth-node)
        meth-name (str meth)
        meth (-> meth-name
                 (subs 1)
                 symbol)
        meth-node (with-meta-of (token-node meth)
                    meth-node)]
    (with-meta-of (list-node (list* (token-node '.) invoked meth-node args))
      expr)))

(defn expand-double-dot
  [_ctx expr]
  (loop [[x form & more] (rest (:children expr))]
    (let [node (with-meta-of (list-node [(token-node '.) x form])
                 expr)]
      (if more
        (recur (cons node more) )
        node))))

(defn find-children
  "Recursively filters children by pred"
  [pred children]
  (mapcat #(if (pred %)
             [(pred %)]
             (when-let [cchildren (:children %)]
               (find-children pred cchildren)))
          children))

(defn fn-args [children]
  (let [args (find-children
              #(and (= :token (tag %))
                    (:string-value %)
                    (when-let [[_ n] (re-matches #"%((\d?\d?)|&)" (:string-value %))]
                      (case n
                        "" 1
                        "&" 0
                        (Integer/parseInt n))))
              children)
        args (sort args)
        varargs? (when-let [fst (first args)]
                   (zero? fst))
        args (seq (if varargs? (rest args) args))
        max-n (last args)
        args (when args (map (fn [i]
                               (symbol (str "%" i)))
                             (range 1 (inc max-n))))]
    {:varargs? varargs?
     :args args}))

(defn expand-fn [{:keys [:children] :as expr}]
  (let [{:keys [:row :col] :as m} (meta expr)
        {:keys [:args :varargs?]} (fn-args children)
        fn-body (with-meta (list-node children)
                  (assoc m
                         :row row
                         :col (inc col)))
        arg-list (vector-node
                  (map #(with-meta (token-node %)
                          {:clj-kondo/mark-used true
                           :clj-kondo/skip-reg-binding true})
                       (if varargs?
                         (concat args '[& %&])
                         args)))
        has-first-arg? (= '%1 (first args))]
    (with-meta
      (list-node [(token-node 'fn*) arg-list
                  fn-body #_(if has-first-arg?
                    let-expr fn-body)])
      (assoc m :clj-kondo.impl/fn-has-first-arg has-first-arg?))))

(defn expand-do-template [_ctx node]
  (let [[_ argv expr & values] (:children node)
        c (count (:children argv))
        argv (:children argv)
        new-node
        (if (pos? c) ;; prevent infinite partition
          (list-node (list* (token-node 'do)
                            (map (fn [a] (walk/postwalk-replace (zipmap argv a) expr))
                                 (partition c values))))
          expr)
        new-node (walk/postwalk #(if (map? %)
                                   (assoc % :clj-kondo.impl/generated true)
                                   %) new-node)]
    new-node))

;;;; Scratch

(comment
  (expand-fn (parse-string "#()"))
  (expand-fn (parse-string "#(println %&)"))
  (expand-fn (parse-string "#(inc %4)"))
  (expand-fn (parse-string "#(inc ^long %)"))
  (expand-fn (parse-string "#(println %2 %&)"))
  (expand-> {} (parse-string "(-> 1 inc inc inc)"))
  (expand->> {} (parse-string "(->> 1 inc inc inc)"))
  (= (parse-string "%")
     (token-node '%))
  )
