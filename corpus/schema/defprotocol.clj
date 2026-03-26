(ns schema.defprotocol
  (:require
   [schema.core :as s]))

(s/defprotocol MyProtocolWithSchema
  "Docstring"
  :extend-via-metadata true
  (^:always-validate method1 :- s/Int
   [this a :- s/Bool]
   [this a :- s/Any b :- s/Str]
   "Method doc2")
  (^:never-validate method2 :- s/Int
   [this a :- s/Str]
   "Method doc2"))

(s/defrecord RecordSchema [ab]
  "Docstring"
  MyProtocolWithSchema
  (method1
    [this a]
    (if a
      0
      ab))
  (method1
    [_this a b]
    ;; "doing cool stuff"
    (if a
      b
      ab))
  (method2
    [_ a]
      ;; "doing cool stuff"
      (count a)))

;;should not work
(->RecordSchema 1 2)
(map->RecordSchema {:ab 1} 2)
(->RecordSchema)

;; should work
(method1 (map->RecordSchema {:ab 1}) true)
(method1 (->RecordSchema 1) :a "test")
(method2 (map->RecordSchema {:ab 1}) 'a)

(def inst (->RecordSchema 1))
(method1 inst true)
