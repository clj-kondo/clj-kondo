(ns multiple-aliases
  (:require [baz.qux :as q]
            [baz.qux :as qq]))

(baz.qux/some-fn 1 2 3)
