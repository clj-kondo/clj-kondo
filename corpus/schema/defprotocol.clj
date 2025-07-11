(ns schema.defprotocol
  (:require
   [schema.core :as s]))

(s/defprotocol MyProtocolWithSchema
  "Docstring"
  :extend-via-metadata true
  (^:always-validate method1 :- s/Int
    [this a :- s/Bool]
    [this a :- s/Any, b :- s/Str]
    "Method doc2")
  (^:never-validate method2 :- s/Int
    [this a :- s/Str]
    "Method doc2"))

(s/defrecord RecordSchema [ab]
  MyProtocolWithSchema
  (method1
    [_ a b]
    (let [_ [a b]]
      ;; "doing cool stuff"
      ab))
  (method2
    [_ a]
      ;; "doing cool stuff"
      2))

;;should not work
(->RecordSchema 1 2)
(map->RecordSchema {:ab 1} 2)

(method1 (->RecordSchema 1) :a "test")
(method2 (map->RecordSchema {:ab 1}) 'a)
