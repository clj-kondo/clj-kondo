(ns interop
  {:no-doc true}
  (:refer-clojure :exclude [eval demunge var?])
  (:require [clojure.string :as str]
            [sci.impl.macros :as macros]
            [sci.impl.types :as t]
            [sci.impl.vars :as vars]
            [sci.lang :as lang]))

(defn dynamic-var
  ([name]
   (dynamic-var name nil (meta name)))
  ([name init-val]
   (dynamic-var name init-val (meta name)))
  ([name init-val meta]
   (let [meta (assoc meta :dynamic true :name (identity name))]
     (sci.lang.Var. init-val name meta false false))))
