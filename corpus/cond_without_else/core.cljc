(ns cljs.core (:refer-clojure :exclude [cond])
    #?(:cljs (:require-macros [cljs.core :as core])))

#?(:cljs (core/defmacro cond [& clauses]))

#?(:cljs (core/cond 1 2))
