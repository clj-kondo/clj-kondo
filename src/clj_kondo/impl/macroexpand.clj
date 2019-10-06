(ns clj-kondo.impl.macroexpand
  {:no-doc true}
  (:require
   [clj-kondo.impl.utils :refer [parse-string tag vector-node list-node
                                 token-node]]
   [clj-kondo.impl.profiler :as profiler]))

(defn expand-> [_ctx expr]
  (profiler/profile
   :expand->
   (let [expr expr
         children (:children expr)
         [c & cforms] (rest children)]
     (loop [x c, forms cforms]
       (if forms
         (let [form (first forms)
               threaded (if (= :list (tag form))
                          (with-meta (list-node (list* (first (:children form))
                                                       x
                                                       (next (:children form)))) (meta form))
                          (with-meta (list-node (list form x))
                            (meta form)))]
           (recur threaded (next forms)))
         x)))))

;; user=> (macroexpand '(cond-> {:a 1} :a :a pos? - neg? (* -1)))
;; (let* [G__150 {:a 1} G__150 (if :a (clojure.core/-> G__150 :a) G__150) G__150 (if pos? (clojure.core/-> G__150 -) G__150)] (if neg? (clojure.core/-> G__150 (* -1)) G__150))

#_(defmacro cond->
  "Takes an expression and a set of test/form pairs. Threads expr (via ->)
  through each form for which the corresponding test
  expression is true. Note that, unlike cond branching, cond-> threading does
  not short circuit after the first true test expression."
  {:added "1.5"}
  [expr & clauses]
  (assert (even? (count clauses)))
  (let [g (gensym)
        steps (map (fn [[test step]] `(if ~test (-> ~g ~step) ~g))
                   (partition 2 clauses))]
    `(let [~g ~expr
           ~@(interleave (repeat g) (butlast steps))]
       ~(if (empty? steps)
          g
          (last steps)))))

(defn expand-cond-> [_ctx expr]
  (let [[_cond->-sym start-expr & clauses] (:children expr)
        g (token-node (gensym))
        steps (map (fn [[test step]] (list-node [(token-node 'if)
                                                 test
                                                 (list-node
                                                  [(token-node '->)
                                                   g
                                                   step])
                                                 g]))
                   (partition 2 clauses))
        ret (list-node [(token-node 'let)
                        (vector-node (list* g start-expr (interleave (repeat g) (butlast steps))))
                        (if (empty? steps) g (last steps))])]
    ;; (prn "RET" ret)
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
                (with-meta
                  (list-node
                   (concat
                    (cons (first (:children form))
                          (next (:children form)))
                    (list x)))
                  (meta form))
                (with-meta (list-node (list form x))
                  (meta form)))]
          (recur threaded (next forms)))
        x))))

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
