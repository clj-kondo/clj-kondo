(ns bar
  {:clj-kondo/config '{:hooks {foo/foo "
                               (fn [{:keys [:sexpr :node]}]
                                 {:sexpr (with-meta `(inc ~@(rest sexpr))
                                           (meta sexpr))})"}}}
  (:require [foo]))

(foo/foo "foo")       ;; error location is at outer sexpr, because inc doesn't have
                      ;; metadata and string can't have metadata
(foo/foo [1 2 3])     ;; this works ok
(foo/foo (inc "foo")) ;; this doesn't work
