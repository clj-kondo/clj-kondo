(ns inline-def)

(defn foo []
  (def x 1))

(defn- foo []
  (def x 1))

(def foo (def x 1))

(deftest foo (def x 1))

(defmacro foo [] (def x 1))
