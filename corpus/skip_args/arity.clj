(ns skip-args.arity)

(defmacro my-macro [a b c]
  `(+ ~a ~b ~c))

(my-macro 1 2 3 (select-keys))
