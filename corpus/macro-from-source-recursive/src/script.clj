(ns script)

(defmacro mylet-pairs
  "Recursive marker macro: each call expands to a `let` of the first
  binding pair, then a recursive call on the rest. Exercises whether
  clj-kondo re-fires the macroexpand hook on the inner self-call."
  {:clj-kondo/macroexpand-hook true}
  [bindings & body]
  (if (seq bindings)
    `(let ~(subvec bindings 0 2)
       (mylet-pairs ~(subvec bindings 2) ~@body))
    `(do ~@body)))

(mylet-pairs [a 1 b 2] (+ a b))
