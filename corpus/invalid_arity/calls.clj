(ns corpus.invalid-arity.calls
  (:require [corpus.invalid-arity.defs
             :as x :refer [public-fixed
                           public-varargs
                           public-multi-arity]]))

(public-fixed 1)
(x/public-fixed 1)
(corpus.invalid-arity.defs/public-fixed 1)
(public-multi-arity 1 2 3)
(public-varargs 1)