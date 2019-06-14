(ns defrecord
  (:import [java.net FileNameMap]))

(defrecord Thing [a b] ;; b should not be reported as unused
  FileNameMap
  (getContentTypeFor [this fileName] (str a "-" fileName)))

(->Thing 1 2 3)
(map->Thing {:a 1} 2)
