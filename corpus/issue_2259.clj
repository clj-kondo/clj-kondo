(ns issue-2259)

(defrecord Example [a b c])

(ns-unmap *ns* '->Example)

(defn ->Example
  [a b c]
  (new Example a b c))
