(ns foo)

(defmacro weird-macro [[_sym _val _opts] & _body]
  ::TODO)

(ns bar
  {:clj-kondo/config '{:macroexpand
                       {foo/weird-macro
                        "
(fn weird-macro [[sym val opts] & body]
  `(let [~sym ~val] ~@(cons opts body)))"}}}
  (:require [foo]))

(foo/weird-macro
 [x :foo {:weird-macro/setting true}]
 (inc x)) ;; type error

(foo/weird-macro) ;; wrong number of args is still reported
