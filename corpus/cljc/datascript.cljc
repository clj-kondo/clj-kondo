(ns datascript
  (:refer-clojure :exclude [seqable?]))

(defn #?@(:clj [seqable?]
          :cljs [^boolean seqable?])
  #?(:clj ^Boolean [x]
     :cljs [x]) x)

(seqable? 1 2)
