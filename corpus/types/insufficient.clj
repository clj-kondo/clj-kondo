(ns corpus.types.insufficient)

(defn foo [a ^String b & args]
  [a b args])

(foo :bad :mkay)

(defmacro my-macro [x])

(my-macro (foo [:not :a :fn-call :so "fine"]))

