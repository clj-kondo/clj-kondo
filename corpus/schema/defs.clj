(ns schema.defs
  (:require [schema.core :as s]))

(s/defn ^:private verify-signature :- (s/maybe s/Int)
  [message :- Str
   [base64-encoded-signature :- Str]
   {:keys [a]} :- {:a s/Int}])

(verify-signature 1 [2] {:a 3}) ;;correct
(verify-signature 1 2) ;; incorrect

(s/defn) ;; doesn't crash the app

