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
                              kns (keyword ns)
                              re-frame-name (name fq-def)
                              id (new-id!)]
                          [(assoc-in
                            ctx
                            [:context kns] {:in-id id})
                           (-> name-expr
                               (assoc :reg fq-def)
                               (assoc-in [:context kns] {:id id :var re-frame-name}))])
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
