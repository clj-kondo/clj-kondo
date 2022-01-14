(ns scratch
  (:require [clj-kondo.main :refer [main]]))

(comment
  (with-in-str "(defn foo [x] (instance? cljs.core.Var x))"
    (main "--lint" "-" "--lang" "cljs"))
  )

