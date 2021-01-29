(ns clj-kondo.impl.macroexpand
  {:no-doc true}
  (:require
   [clj-kondo.impl.utils :refer [parse-string tag vector-node list-node token-node]]))

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
  [_ctx expr]
  (let [[cond->-sym start-expr & clauses] (:children expr)
        thread-sym (case (name (:value cond->-sym))
                     "cond->" 'clojure.core/->
                     "cond->>" 'clojure.core/->>)
        g (with-meta-of (token-node (gensym))
            start-expr)
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
        gx (with-meta-of (token-node (gensym "_")) x)
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
        ctor-name (name ctor)
        ctor-name (-> ctor-name
                      (subs 0 (dec (count ctor-name)))
                      symbol)
        ctor-node (with-meta-of (token-node ctor-name)
                    ctor-node)]
    (list-node (list* (token-node 'new) ctor-node children))))

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
                  {:row row
                   :col (inc col)})
        arg-list (vector-node
                  (map #(with-meta (token-node %)
                          {:clj-kondo/mark-used true})
                       (if varargs?
                         (concat args '[& %&])
                         args)))
        has-first-arg? (= '%1 (first args))
        let-expr (when has-first-arg?
                   (list-node
                    [(token-node 'clojure.core/let*)
                     (vector-node
                      [(with-meta
                         (token-node '%)
                         {:clj-kondo/mark-used true})
                       (token-node '%1)])
                     fn-body]))]
    (with-meta
      (list-node [(token-node 'fn*) arg-list
                  (if has-first-arg?
                    let-expr fn-body)])
      m)))

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
