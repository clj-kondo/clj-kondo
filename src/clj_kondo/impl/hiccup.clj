(ns clj-kondo.impl.hiccup
  (:require
   [clj-kondo.impl.analyzer.common :as common]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.utils :as utils]))

(defn lint-hiccup [ctx children]
  (let [ctx (assoc ctx :hiccup true)]
    (loop [children (seq children)
           i 0]
      (when children
        (let [fst (first children)]
          (when (and (= :map (utils/tag fst))
                     (not= 1 i))
            (findings/reg-finding! ctx (merge {:type :hiccup
                                               :message "Hiccup attribute map in wrong location"
                                               :filename (:filename ctx)}
                                              (meta fst)))))
        (recur (next children) (inc i))))
    (common/analyze-children (update ctx
                                     :callstack #(cons [nil :vector] %))
                             children)))
