(ns clj-kondo.impl.analyzer.core-async
  (:require
   [clj-kondo.impl.utils :refer [tag]]))

(defn analyze-alt-val [ctx analyze-expression** extract-bindings expr]
  (let [[fst-child & rest-children] (:children expr)]
    (if (and
         fst-child
         (= :list (tag expr))
         (= :vector (tag fst-child)))
      ;; analyze syntax like: ([v ch] 'do_something_with_v_or_ch)
      ;; NOTE: this syntax also supports destructuring:
      ;; (async/alt!! (doto (async/chan) (async/put! {:a 1}))  ([{:keys [:a]} ch] [a ch]))
      (let [bindings (extract-bindings ctx fst-child)
            ctx (update ctx :bindings into bindings)]
        (mapcat #(analyze-expression** ctx %) rest-children))
      (analyze-expression** ctx expr))))

(defn analyze-alt! [ctx expr]
  (let [{:keys [:analyze-expression**
                :extract-bindings]} ctx
        children (next (:children expr))
        pairs (partition-all 2 children)]
    (for [[k v] pairs
          analyzed-value (concat (analyze-expression** ctx k)
                                 (analyze-alt-val ctx analyze-expression**
                                                  extract-bindings v))]
      analyzed-value)))

(comment
  )
