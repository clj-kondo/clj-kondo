(ns foobar)

(defmacro foo [x]
  x)

(foo
 (do #_:clj-kondo/ignore
     (prn (+ 1 2 3))
     (inc :foo)))
