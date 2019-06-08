(ns defrecord
  (:import [java.net FileNameMap]))

(defrecord Thing [a b]
  FileNameMap
  (getContentTypeFor [this fileName] (str a "-" fileName)))

(->Thing 1 2 3)
(map->Thing {:a 1} 2)
