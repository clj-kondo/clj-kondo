(ns clj-kondo.impl.types.utils
  {:no-doc true})

(defn call [x]
  (when (:call x)
    x))

(defn union-type [x y]
  (cond (or (identical? x :any)
            (identical? y :any)) :any
        (set? x)
        (if (set? y)
          (into x y)
          (conj x y))
        :else (hash-set x y)))
