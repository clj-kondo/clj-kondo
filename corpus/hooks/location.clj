(ns bar
  {:clj-kondo/config '{:hooks {foo/foo "
                               (fn [{:keys [:sexpr :node]}]
                                 {:sexpr `(inc ~@(rest sexpr))`})"}}}
  (:require [foo]))

(foo/foo "foo")
