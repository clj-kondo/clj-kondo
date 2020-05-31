(ns foo)

(defmacro weird-macro [[_sym _val _opts] & _body]
  ::TODO)

(ns bar
  {:clj-kondo/config '{:macroexpand
                       {foo/weird-macro
                        "
(fn weird-macro [{:keys [:sexpr]}]
  (let [[[sym val opts] & body] (rest sexpr)]
    (when-not (and sym val)
      (throw (ex-info \"No sym and val provided\" {})))
    {:sexpr `(let [~sym ~val] ~@(cons opts body))}))
"}}}
  (:require [foo]))

(foo/weird-macro
 [x :foo {:weird-macro/setting true}]
 (inc x)) ;; type error

(foo/weird-macro) ;; wrong number of args is still reported

(ns slingshot
  {:clj-kondo/config '{:macroexpand
                       {slingshot.slingshot/try+
                        "
(defn expand-catch [[_catch catchee & exprs :as expr]]
  (cond (vector? catchee)
        (let [[selector & exprs] exprs]
           ;; (debug \"meta\" (map (juxt identity (comp meta first)) exprs))
            `(catch Exception _e#
                        (let [~selector nil]
                          ~@exprs)))
        :else expr))

(fn try+ [{:keys [sexpr]}]
  ;; (prn \"expr\" sexpr)
  (let [body (rest sexpr)
        [body catches]
        (loop [body body
               body-exprs []
               catches []]
          (if (seq body)
            (let [f (first body)]
              (if (and (seq? f) (= 'catch (first f)))
                (recur (rest body)
                       body-exprs
                       (conj catches (expand-catch f)))
                (recur (rest body)
                       (conj body-exprs f)
                       catches)))
            [body-exprs catches]))]
    {:sexpr
      `(let [~'throw+ (fn [])
           ~'&throw-context nil]
       ~(with-meta `(try ~@body ~@catches)
          (meta sexpr)))}))
"}}}
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
