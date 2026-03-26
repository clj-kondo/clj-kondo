(ns defrecord
  (:import [java.net FileNameMap]))

(defprotocol IFoo
  (bar [_]))

(defrecord Dude []
  IFoo        ;; missing-protocol-method
  (barx [_])) ;; unresolved-protocol-method

(defrecord Thing [a b] ;; b should not be reported as unused
  FileNameMap
  (getContentTypeFor [this fileName] (str a "-" fileName)))

(->Thing 1 2 3)
(map->Thing {:a 1} 2)
