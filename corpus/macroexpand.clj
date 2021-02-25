(ns foo)

(defmacro weird-macro [[_sym _val _opts] & _body]
  ::TODO)

(ns bar
  {:clj-kondo/config
   '{:hooks
     ;; the macro expansion code can be found in
     ;; .clj-kondo/macroexpand/weird_macro.clj
     {:analyze-call {foo/weird-macro macroexpand.weird-macro/weird-macro}}}}
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
     {:analyze-call {slingshot.slingshot/try+ macroexpand.try-plus/try+}}}}
  (:require [log :as log]
            [slingshot.slingshot :refer [try+ throw+]]))

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
  {:clj-kondo/config '{:hooks {:analyze-call {better.cond/cond hooks.better-cond/cond}}}}
  (:require [better.cond :as b]))

(let [x 10]
  (b/cond
    (= x 1) true
    :let [y (inc x)]      ;; binding is recognized
    (= 11 y) (subs y 0))) ;; yay, type error because y is not a string

(ns quux
  {:clj-kondo/config '{:hooks {:analyze-call {rum.core/defc hooks.rum/f
                                              rum.core/defcs hooks.rum/f}}}}
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
