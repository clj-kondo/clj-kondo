(ns myns)

(defmacro my-when
  "Reader-conditional-free body; extracted on the :clj feature pass."
  {:clj-kondo/macroexpand-hook true}
  [test & body]
  `(if ~test (do ~@body) nil))

(my-when true 42)
