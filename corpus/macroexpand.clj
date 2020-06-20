(ns foo)

(defmacro weird-macro [[_sym _val _opts] & _body]
  ::TODO)

(ns bar
  {:clj-kondo/config
   '{:hooks
     ;; the macro expansion code can be found in
     ;; .clj-kondo/macroexpand/weird_macro.clj
     {foo/weird-macro macroexpand.weird-macro/weird-macro}}}
  (:require [foo]))

(foo/weird-macro
 [x :foo {:weird-macro/setting true}]
 (inc x) ;; type error
 (inc "foo") ;; this works although strings can't carry metadata. this is why we're using the rewrite api now!
 )

(foo/weird-macro) ;; wrong number of args is still reported

(ns slingshot
  {:clj-kondo/config
   '{:hooks
     {slingshot.slingshot/try+ macroexpand.try-plus/try+}}}
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
  {:clj-kondo/config '{:hooks {better.cond/cond
                               "
(require '[clj-kondo.hooks-api :as api])
(defn process-pairs [pairs]
  (loop [[[lhs rhs :as pair] & pairs] pairs
         new-body [(api/token-node 'cond)]]
    (if pair
      (let [lhs-sexpr (api/sexpr lhs)]
      (cond
        (= 1 (count pair)) (api/list-node (conj new-body lhs))
        (not (keyword? lhs-sexpr))
        (recur pairs
               (conj new-body lhs rhs))
        (= :let lhs-sexpr)
        (api/list-node (conj new-body (api/token-node :else) (api/list-node [(api/token-node 'let) rhs (process-pairs pairs)])))))
      (api/list-node new-body))))

(def f
  (fn [{:keys [:node]}]
    (let [expr (let [args (rest (:children node))
                     pairs (partition-all 2 args)]
                 (process-pairs pairs))]
      {:node (with-meta expr
                (meta node))})))"}}}
  (:require [better.cond :as b]))

(let [x 10]
  (b/cond
    (= x 1) true
    :let [y (inc x)]      ;; binding is recognized
    (= 11 y) (subs y 0))) ;; yay, type error because y is not a string

(ns quux
  {:clj-kondo/config '{:hooks {rum.core/defc "
(require '[clj-kondo.hooks-api :as api])
(def f (fn [{:keys [:node]}]
         (let [args (rest (:children node))
               component-name (first args)
               args (next args)
               body
               (loop [args* args
                      mixins []]
                 (if (seq args*)
                   (let [a (first args*)]
                     (if (vector? (api/sexpr a))
                       (cons a (concat mixins (rest args*)))
                       (recur (rest args*)
                              (conj mixins a))))
                   args))
              new-node (with-meta
                         (api/list-node (list* (api/token-node 'defn) component-name body))
                          (meta node))]
           ;; (prn (meta sexpr))
           ;; (prn expr)
           {:node new-node})))
"}
                       :lint-as {rum.core/defcs rum.core/defc}}}
  (:require [rum.core :as rum]))

(rum/defc with-mixin
  < rum/static
  [context] ;; no unresolved symbol
  [:div
   [:h1 "result"]
   [:pre (pr-str context)]])

(with-mixin 1)
(with-mixin a a a a) ;; unresolved symbol and arity error for with-mixin

(rum/defc with-mixin ;; redefined var
  [_])

(rum/defcs stateful < (rum/local 0 ::key)
  [state label]
  (let [local-atom (::key state)]
    [:div { :on-click (fn [_] (swap! local-atom inc)) }
     label ": " @local-atom]))

(stateful {} 1) ;; no warning
