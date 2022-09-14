(ns excluded-alias
  (:require [baz.qux :as q]
            [clojure.string :as str]))

(baz.qux/some-fn 1 2 3)
(clojure.string/join ", " (range 10))
