(ns script
  (:require [clojure.string :as str]))

(defmacro my-let
  {:clj-kondo/macro true}
  [bnds & body]
  `(let [~@bnds] ~@body))

(defmacro shout
  {:clj-kondo/macro true}
  [s]
  `(str/upper-case ~s))

(my-let [x 1] (inc x))
(shout "hi")
