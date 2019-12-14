(ns schema.defrecord
  (:require [schema.core :as s]))

(s/defrecord StampedNames
    [date :- Long
     names :- [s/Str]])

(s/defn stamped-names :- StampedNames
  [names :- [s/Str]]
  (StampedNames. (str (System/currentTimeMillis)) names))

(stamped-names ["foo" "bar"])
