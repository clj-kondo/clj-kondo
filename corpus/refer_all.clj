(ns funs)

(defn foo [_])
(defn bar [_])
(defn baz [_])

(ns refer-all
  (:require [funs :refer :all :exclude [baz] :rename {bar new-name}] :reload))

(foo) ;; caught
(new-name) ;; caught
(baz) ;; unrecognized call

(bar) ;; not recognized
