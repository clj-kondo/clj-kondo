(ns failing-macro)

(defmacro broken
  {:clj-kondo/macroexpand-hook true}
  [x]
  (undefined-helper x))

(broken 42)
(broken 99)
