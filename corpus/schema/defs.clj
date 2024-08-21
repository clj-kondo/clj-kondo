(ns schema.defs
  (:require [schema.core :as s]))

(s/defn ^:private verify-signature :- (s/maybe s/Int)
  [message :- s/Str
   [base64-encoded-signature :- s/Str]
   {:keys [a]} :- {:a s/Int}])

(verify-signature 1 [2] {:a 3}) ;;correct
(verify-signature 1 2) ;; incorrect

(s/defn) ;; doesn't crash the app

;; from kekkonen
(s/defn handler
  ([meta :- s/Any]
   (handler (dissoc meta :handle) (:handle meta)))
  ([meta :- s/Any f :- s/Any]
   (assert (:name meta) "handler should have :name")
   (vary-meta f merge {:type :handler} meta)))

(handler {}) ;; this should be OK

(s/defn bar :- #(last %)
  [x]
  x)

(s/def foo :- long "a long" 2)

(schema.core/defn baz []
  (str {:a 1 :b}))

(s/defn bad-return1 []
  :- s/Int
  1)

(s/defn bad-return2
  ([]
   :- s/Int
   1))

(s/defn bad-return3
  (:- s/Int []
   1))

(s/defn :- s/Int bad-return4
  []
  1)

(s/defn bad-return5 ;;FIXME does this work?
  "foo"
  :- s/Int 
  []
  1)

(s/defn good-return [] :-)
