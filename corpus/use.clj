(ns use)

(ns foo1
  (:use [clojure.string])) ;; this message contains join

join

(ns foo1b
  (:use clojure.string)) ;; this message contains join

join

(ns foo1c
  (:use clojure.set)) ;; this message also contains join, but from clojure set

join

(ns foo2
  (:use [clojure.string :only [join]]))

(ns foo2b)
(use '[clojure.string :only [join]])

(ns foo4)
(use 'clojure.set)
join

(ns foo6)
(use '[clojure.string])
join

