(ns hooks.coerce-sexpr-roundtrip
  (:require [myns :refer [defmodel]]))

(new String "\\s+")
(defmodel Foobar :table)
