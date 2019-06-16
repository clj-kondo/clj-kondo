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
