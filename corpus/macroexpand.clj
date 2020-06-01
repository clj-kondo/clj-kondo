(ns foo)

(defmacro weird-macro [[_sym _val _opts] & _body]
  ::TODO)

(ns bar
  {:clj-kondo/config
   '{:macroexpand
     ;; the macro expansion code can be found in
     ;; .clj-kondo/macroexpand/weird_macro.clj
     {foo/weird-macro "macroexpand/weird_macro.clj"}}}
  (:require [foo]))

(foo/weird-macro
 [x :foo {:weird-macro/setting true}]
 (inc x)) ;; type error

(foo/weird-macro) ;; wrong number of args is still reported

(ns slingshot
  {:clj-kondo/config
   '{:macroexpand
     {slingshot.slingshot/try+ "macroexpand/try_plus.clj"}}}
  (:require [log :as log]
            [slingshot.slingshot :refer [try+]]))

(try+
 (inc 1)
 (catch [:type :tensor.parse/bad-tree] {:keys [tree hint]} ;; unused
   (log/error "failed to parse tensor" "with hint" hint)
   (throw+)) ;; throw+ is known
 (catch Object _
   (log/error (:throwable &throw-context) ;; &throw-context is known
              "unexpected error")
   (throw+)))

(try+) ;; try without catch

(ns baz
  {:clj-kondo/config '{:macroexpand {better.cond/cond
                                     "
(defn process-pairs [pairs]
  (loop [[[lhs rhs :as pair] & pairs] pairs
         new-body ['cond]]
    (if pair
      (cond
        (= 1 (count pair)) (seq (conj new-body lhs))
        (not (keyword? lhs))
        (recur pairs
               (conj new-body lhs rhs))
        (= :let lhs)
        (seq (conj new-body :else (list 'let rhs
                                       (process-pairs pairs)))))
      (seq new-body))))

(def f
  (fn [{:keys [:sexpr]}]
    (let [expr (let [args (rest sexpr)
                     pairs (partition-all 2 args)]
                 (process-pairs pairs))]
      {:sexpr (with-meta expr
                (meta sexpr))})))"}}}
  (:require [better.cond :as b]))

(let [x 10]
  (b/cond
    (= x 1) true
    :let [y (inc x)]      ;; binding is recognized
    (= 11 y) (subs y 0))) ;; yay, type error because y is not a string
