(ns clojure.core)

(defn get [x y] x)

(ns corpus.exclude-clojure1
  (:refer-clojure :exclude [get]))

(get 1 2 3 4)

(ns corpus.exclude-clojure2)

(get {} 2 3 4)
