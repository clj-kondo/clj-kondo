(ns clj-kondo.impl.types
  {:no-doc true}
  (:require [clj-kondo.impl.clojure.spec.alpha :as s]))

(s/def ::number number?)
(def specs {'clojure.core {'inc {:args (s/cat :x ::number)}}})

