(ns clj-kondo.impl.analyzer.babashka
  {:no-doc true}
  (:require
    [clj-kondo.impl.analyzer.common :as common]
    [clj-kondo.impl.utils :as utils]))

(defn analyze-$ [ctx expr]
  (let [[child & children] (rest (:children expr))
        [opts children] (if (= :map (utils/tag child))
                          [child children]
                          [nil (cons child children)])
        children (doall (keep (fn [child]
                                (let [s (utils/sexpr child)]
                                  (when (and (seq? s)
                                             (= 'clojure.core/unquote (first s)))
                                    (first (:children child)))))
                              children))
        children (if opts
                   (cons opts children)
                   children)]
    (common/analyze-children ctx children)))
