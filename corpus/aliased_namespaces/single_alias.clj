(ns single-alias
  (:require [baz.qux :as q]))

(baz.qux/some-fn 1 2 3)
