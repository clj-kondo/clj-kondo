(ns foobar (:require clojure.template))

(clojure.template/do-template [_] #a/b 1 2)

(defmacro dude [_])

(dude (clojure.core/read-string "#a/b 1 2"))
