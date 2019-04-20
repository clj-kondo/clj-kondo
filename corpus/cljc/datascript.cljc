(ns datascript.db
  (:refer-clojure :exclude [seqable?]))

(defn #?@(:clj  [^Boolean seqable?]
          :cljs [^boolean seqable?])
  [x] x)

(seqable? 1 2)
