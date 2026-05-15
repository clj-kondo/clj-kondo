(ns failing-macro)

(defmacro broken
  {:clj-kondo/macro true}
  [x]
  (undefined-helper x))

(broken 42)
(broken 99)
