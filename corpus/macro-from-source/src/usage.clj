(ns usage
  (:require [script]))

(script/my-let [x 1] (inc x))
(script/shout "hello")
(script/joined ["a" "b"])
(script/tagged 1)
(script/mixed "Hi")
(script/literal :ignored)
(script/setty #{1} #{2})
(script/defdouble forty-two 21)
(inc forty-two)
