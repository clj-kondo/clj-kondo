(ns foobar
  #?(:cljs (:require-macros [foobar :refer [dude]])))

#?(:clj
   (defmacro dude [_]
     (if (:ns &env)
       `(inc 1 2 3)
       `(inc 1 2))))

(dude x)
