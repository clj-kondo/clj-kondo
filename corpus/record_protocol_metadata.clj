(ns record-protocol-metadata
  (:import (com.newrelic.api.agent Trace))) ;; <= Trace flagged as unused import

(defprotocol Handler
  (handle [this req] "Handle a Ring request."))

(defrecord NewRelicHandler [handler]
  Handler
  (^{Trace {:dispatcher true :transactionType "web"}} handle [_this req] ;; <= Trace used here as a Java Annotation
   (handler req)))
