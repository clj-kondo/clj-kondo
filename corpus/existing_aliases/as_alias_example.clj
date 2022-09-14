(ns as-alias-example
  (:require [baz.qux :as-alias q]))

(identity ::baz.qux/some-fn)
