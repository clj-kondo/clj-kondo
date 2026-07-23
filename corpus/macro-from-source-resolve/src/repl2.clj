(ns repl2
  (:require [repl]))

(repl/try+
 (identity
  (throw (Exception.)))
 (repl/catch-all e (print (str "caught: " e))))
