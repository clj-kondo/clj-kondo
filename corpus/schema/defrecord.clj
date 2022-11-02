(ns schema.defrecord
  (:require [schema.core :as s]))

(s/defrecord StampedNames
    [date :- Long
     names :- [s/Str]])

(s/defn stamped-names :- StampedNames
  [names :- [s/Str]]
  (StampedNames. (str (System/currentTimeMillis)) names))

(stamped-names ["foo" "bar"])

->StampedNames
map->StampedNames

(s/defrecord Record2
    [_]
  {:a s/Int
   :b s/Int}) ;; ok

(s/defrecord ErrorRecord
    [x]
  {(s/optional-key :a) s/Int
   (s/optional-key :b) s/Int} ;; ok
  (fn [x] x) ;; extra validator
  clojure.lang.IFn
  (applyTo [_ xs] (apply + x xs)))
