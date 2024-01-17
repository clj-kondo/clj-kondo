(ns a
  (:import [java.lang Long]))

(defmacro my-defrecord [& body]
  `(defrecord ~@body))

(my-defrecord foo [^Long x])
