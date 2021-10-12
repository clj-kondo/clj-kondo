(ns def-fn (:refer-clojure :exclude [cons]))

(def
  ^{:arglists '([x seq])
    :doc "Returns a new seq where x is the first element and seq is
    the rest."
    :added "1.0"
    :static true}

  cons (fn* ^:static cons [x seq] (. clojure.lang.RT (cons x seq))))

(cons 1 2 3)
