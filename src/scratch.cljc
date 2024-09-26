(ns scratch
  (:require
   [clojure.spec.alpha :as s]
   #?(:clj  [clojure.spec.gen.alpha :as gen]
      :cljs [cljs.spec.gen.alpha :as gen])))

(s/def ::break
  (s/with-gen
    nat-int?
    #(gen/fmap
      inc
      (s/gen integer?))))
