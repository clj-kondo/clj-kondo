(ns clj-kondo.impl.analyzer.re-frame
  {:no-doc true}
  (:require
     [clj-kondo.impl.analyzer.common :as common]
     [clj-kondo.impl.utils :as utils]))

(def counter (atom 0))

(defn new-id! []
  (str (swap! counter inc)))

(defn analyze-reg [ctx expr fq-def]
  (let [[name-expr & body] (next (:children expr))
        [ctx reg-val] (if (:k name-expr)
                        (let [ns (namespace fq-def)
                              re-frame-name (name fq-def)
                              id (new-id!)
                              in-reg-id-key (keyword ns (format "in-%s-id" re-frame-name))
                              reg-id-key (keyword ns (str re-frame-name "-id"))]
                          [(assoc-in
                            ctx
                            [:context in-reg-id-key] id)
                           (-> name-expr
                               (assoc-in [:context reg-id-key] id))])
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
