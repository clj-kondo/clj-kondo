(ns lint-as--GT
  {:clj-kondo/config {:lint-as {lint-as--GT/injecting-thread-first clojure.core/->}}})

(defmacro injecting-thread-first
  [& body]
  `(-> :inject ~@body))

(defn arity-2 [x y]
  [x y])

(injecting-thread-first (arity-2 :y)
                        (arity-2 :foo))
