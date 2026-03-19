(ns aot-test.sample)

(defn one-arg [x] x)

(defn two-args [x y] [x y])

(defn three-args [x y z] [x y z])

(defn varargs [x & more] (cons x more))

(defn- private-fn [x] x)

(defmacro sample-macro [& body] `(do ~@body))

(def a-value 42)
