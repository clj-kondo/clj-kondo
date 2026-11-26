(ns deftype)

(deftype LazySeq2 [meta ^:mutable fn ^:mutable s ^:mutable __hash]
  Object
  (toString [coll]
    (pr-str* coll))
  (equiv [this other]
    (-equiv this other))
  (sval [coll]
    (if (nil? fn)
      s
      (do
        (set! s (fn))
        (set! fn nil)
        s)))
  IPending
  (-realized? [coll]
    (not fn)))
