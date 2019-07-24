(ns funs)

(defn foo [_])
(defn bar [_])
(defn baz [_])

(ns another-funs)

(defn another-foo [])

(ns refer-all
  (:require [funs :refer :all :exclude [baz] :rename {bar new-name}] :reload
            [another-funs :refer :all]))

(foo) ;; caught
(new-name) ;; caught
(baz) ;; unrecognized call

(bar) ;; not recognized

(another-foo) ;; should be resolved without warning
