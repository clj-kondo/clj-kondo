(ns java.usages (:import [clojure.lang PersistentVector]))

(try (catch Exception foo foo))
Thread/sleep
(Thread/sleep 100)
(Thread. (fn []))
PersistentVector

(import '[clojure.lang Compiler])
Compiler/specials
foo.bar.Baz
foo.bar.Baz/EMPTY
java.util.Date
(java.io.File/createTempFile "foo" "bar")
