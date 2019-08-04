(ns clj-kondo.impl.analyzer.core-async
  (:require
   [clj-kondo.impl.utils :refer [tag]]))

(defn analyze-alt-val [ctx analyze-expression** analyze-fn-body expr]
  (let [[fst-child _] (:children expr)]
    (if (and
         fst-child
         (= :list (tag expr))
         (= :vector (tag fst-child)))
      (:parsed (analyze-fn-body ctx expr))
      (analyze-expression** ctx expr))))

(defn analyze-alt! [ctx expr]
  (let [{:keys [:analyze-expression**
                :analyze-fn-body]} ctx
        children (next (:children expr))
        pairs (partition-all 2 children)]
    (for [[k v] pairs
          analyzed-value (concat (analyze-expression** ctx k)
                                 (analyze-alt-val ctx analyze-expression**
                                                  analyze-fn-body v))]
      analyzed-value)))

(comment
  )
