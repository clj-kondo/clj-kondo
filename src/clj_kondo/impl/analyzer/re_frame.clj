(ns clj-kondo.impl.analyzer.re-frame
  {:no-doc true}
  (:require
     [clj-kondo.impl.analyzer.common :as common]
     [clj-kondo.impl.utils :as utils]))

(defn analyze-reg [ctx expr fq-def]
  (let [[name-expr & body] (next (:children expr))
        [ctx reg-val] (if-let [kw (:k name-expr)]
                        [(assoc-in ctx [:context fq-def] (meta name-expr))
                         (-> name-expr
                             (assoc :reg fq-def)
                             (assoc-in [:context fq-def] true))]
                        [ctx  name-expr])]
    (common/analyze-children ctx (cons reg-val body))))

;;;; Scratch

(comment
  (require '[clj-kondo.core :as clj-kondo])
  (-> (with-in-str "(require '[re-frame.core :as re-frame]) (re-frame/reg-sub ::foo (fn [_]))"
        (clj-kondo/run! {:lang :cljs :lint ["-"] :config {:output {:analysis {:keywords true}}}}))
      :analysis :keywords)

  (-> (with-in-str "(require '[re-frame.core :as re-frame]) (re-frame/reg-sub ::foo (fn [x] (inc x)))"
        (clj-kondo/run! {:lang :cljs :lint ["-"] :config {:output {:analysis {:keywords true}}}}))
      :analysis :var-usages)

  )
