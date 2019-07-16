(ns rename
  (:refer-clojure :rename {update core-update} :exclude [conj])
  (:require [clojure.string :rename {join foo}]))
#?(:clj (core-update)) ;; <- arity error
#?(:cljs (core-update)) ;; <- arity error
#?(:clj conj) ;; <- unresolved
#?(:cljs conj) ;; <- unresolved
#?(:clj (join)) ;; <- unresolved
#?(:cljs (join)) ;; <- unresolved
#?(:clj (foo)) ;; <- arity error
#?(:cljs (foo)) ;; <- arity error
