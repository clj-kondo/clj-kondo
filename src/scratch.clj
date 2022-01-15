(ns scratch
  (:require [clj-kondo.main :refer [main]]))

(comment
  (with-in-str "(ns foo (:require [foo])) (let [foo #js {}] foo.bar)"
    (main "--lint" "-" "--lang" "cljs"))
  (with-in-str "(ns foo (:require [foo :refer [baz]])) (def quux) foo.bar baz.dude"
    (main "--lint" "-" "--lang" "cljs"))
  (with-in-str "(defn foo [x] (instance? cljs.core.Var x))"
    (main "--lint" "-" "--lang" "cljs"))
  )

