(ns myns)

(defmacro maybe-foo
  "Compares an auto-resolved current-ns keyword. The naive extraction would
  put `::foo` literally into the gen ns, where SCI resolves it to a
  different namespace and the comparison breaks. The rewrite emits the
  fully-qualified literal so behavior matches the source."
  {:clj-kondo/macroexpand-hook true}
  [x]
  (if (= ::foo x)
    'matched
    'not-matched))

(maybe-foo :myns/foo)
