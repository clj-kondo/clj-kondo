(ns corpus.invalid-arity.private-calls
  (:require [corpus.invalid-arity.private-defs :as x :refer [private]]))

(private 1 2 3)
