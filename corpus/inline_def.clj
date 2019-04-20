(ns inline-def
  (:require [clojure.test :as t]))

(defn foo []
  (def x 1))

(defn- foo []
  (def x 1))

(def foo (def x 1))

(t/deftest foo (def x 1))

(defmacro foo [] (def x 1))
