(ns lint-as-for
  {:clj-kondo/config '{;; TODO: setting the level doesn't yet work in ns local
                       ;; configs, see #430, so we have to set it in our tests
                       :linters {:unresolved-symbol {:level :warning}}
                       :lint-as {plumbing.core/for-map clojure.core/for
                                 clojure.java.jdbc/with-db-transaction clojure.core/let}}})

(require 'plumbing.core)

(plumbing.core/for-map [[k v] {} :let [[v & _rst] v]] [k v])

(for [[k v] {} :let [[v & _rst] v]] [k v])

;; the fix for #434 does not result in a redundant let warning for:
(require 'clojure.java.jdbc)
(let [_a 1 d 2]
  (clojure.java.jdbc/with-db-transaction [db d] db))
