(ns clj-kondo.impl.analyzer.spec
  (:require
   [clj-kondo.impl.utils :as utils]
   [clj-kondo.impl.findings :as findings]))

(defn analyze-fdef [{:keys [:analyze-children] :as ctx} expr]
  (let [[sym & body] (next (:children expr))]
    (when-not (utils/symbol-token? sym)
      (findings/reg-finding! (:findings ctx)
                             (utils/node->line (:filename ctx)
                                               sym
                                               :error
                                               :syntax
                                               "expected symbol")))
    (analyze-children ctx body)))
