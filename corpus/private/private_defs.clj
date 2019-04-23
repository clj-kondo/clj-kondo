(ns corpus.invalid-arity.private-defs)

(defn- private [x y z] x)
(private 1 2 3)

(defn ^:private private-by-meta [])
(private-by-meta)
