(ns lint-as-for
  {:clj-kondo/config '{;; TODO: setting the level doesn't yet work in ns local
                       ;; configs, see #430, so we have to set it in our tests
                       :linters {:unresolved-symbol {:level :warning}}
                       :lint-as {plumbing.core/for-map clojure.core/for}}})

(require 'plumbing.core)

(plumbing.core/for-map [[k v] {} :let [[v & _rst] v]] [k v])

(for [[k v] {} :let [[v & _rst] v]] [k v])
