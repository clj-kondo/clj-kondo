(ns usage
  (:require [script]))

(script/my-let [x 1] (inc x))
(script/shout "hello")
