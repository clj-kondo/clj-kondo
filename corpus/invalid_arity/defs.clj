(ns corpus.invalid-arity.defs)

(defn public-fixed [x y z] x)
(defn public-multi-arity ([x] (public-multi-arity x false)) ([x y] x))
(defn public-varargs [x y & zs] x)

(public-fixed 1)
(public-multi-arity 1)
(public-multi-arity 1 2)
(public-multi-arity 1 2 3)
(public-varargs 1)
