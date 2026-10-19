(ns usage
  (:require [qualification]))

(qualification/defcheck-wrapper example)
example
(qualification/defjoined joined ["a" "b"])
joined
(qualification/quoted-symbol)
