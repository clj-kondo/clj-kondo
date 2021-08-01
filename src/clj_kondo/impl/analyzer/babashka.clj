(ns clj-kondo.impl.analyzer.babashka
  {:no-doc true}
  (:require
    [clj-kondo.impl.analyzer.common :as common]
    [clj-kondo.impl.utils :as utils]))


(defn analyze-$ [ctx expr]
  (let [children (doall (keep (fn [child]
                                (let [s (utils/sexpr child)]
                                  (when (and (seq? s)
                                             (= 'clojure.core/unquote (first s)))
                                    (first (:children child)))))
                              (:children expr)))]
    (common/analyze-children ctx children)))
