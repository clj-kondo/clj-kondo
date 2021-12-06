(ns clj-kondo.impl.analyzer.re-frame
  {:no-doc true}
  (:require
     [clj-kondo.impl.analyzer.common :as common]
     [clj-kondo.impl.utils :as utils]
     [clojure.walk :as w]))

(def counter (atom 0))

(defn new-id! []
  (str (swap! counter inc)))

(defn- with-context [ctx name-expr fq-def]
  (if (:k name-expr)
    (let [ns (namespace fq-def)
          kns (keyword ns)
          re-frame-name (name fq-def)
          id (new-id!)]
      [(assoc-in
        ctx
        [:context kns :in-id] id)
       (-> name-expr
           (assoc :reg fq-def)
           (assoc-in [:context kns] {:id id :var re-frame-name}))])
    [ctx name-expr]))

(defn analyze-reg [ctx expr fq-def]
  (let [[name-expr & body] (next (:children expr))
        [ctx reg-val] (with-context ctx name-expr fq-def)]
    (common/analyze-children ctx (cons reg-val body))))

(defn analyze-subscribe [ctx expr ns]
  (let [kns (keyword ns)
        [farg & args :as children] (next (:children expr))]
    (if (identical? :vector (utils/tag farg))
      (let [[subscription-id & subscription-params] (:children farg)]
        (common/analyze-children (assoc-in ctx [:context kns :subscription-ref] true) [subscription-id])
        (when subscription-params
          (common/analyze-children ctx subscription-params))
        (when args
          (common/analyze-children ctx args)))
      (common/analyze-children ctx children))))

(defn analyze-dispatch [ctx expr ns]
  (let [kns (keyword ns)
        [event-id & event-params] (:children (first (next (:children expr))))]
    (common/analyze-children (assoc-in ctx [:context kns :event-ref] true) [event-id])
    (when event-params
      (common/analyze-children ctx event-params))))

(defn analyze-reg-sub [ctx expr fq-def]
  (let [[name-expr & body] (next (:children expr))
        arrow-subs (map last (filter #(= :<- (:k (first %))) (partition-all 2 body)))]
    (if (seq arrow-subs)
      (let [[ctx reg-val] (with-context ctx name-expr fq-def)]
        (doseq [s arrow-subs]
          (analyze-subscribe ctx {:children (cons :<- [s])} (str (namespace fq-def))))
        (common/analyze-children ctx (cons reg-val (take-last 1 body))))
      (analyze-reg ctx expr fq-def))))

(defmulti analyze-dispatch-type (fn [_ctx _fq-def x] (:k (first x))))

(defmethod analyze-dispatch-type :dispatch [ctx fq-def x]
  (let [[disp-kw & _dispatch-vector] x]
    (analyze-dispatch ctx {:children x} (str (namespace fq-def)))
    [disp-kw]))

(defmethod analyze-dispatch-type :dispatch-n [ctx fq-def x]
  (let [disp-kw (:k (first x))]
    (doseq [dispatch-vector (:children (first (drop 1 x)))]
      (analyze-dispatch ctx {:children (cons disp-kw [dispatch-vector])} (str (namespace fq-def))))
    [disp-kw]))

(defmethod analyze-dispatch-type :dispatch-later [ctx fq-def x]
  (let [disp-kw (:k (first x))]
    (analyze-dispatch
     ctx
     {:children (->> (last x)
                     :children
                     (partition-all 2)
                     (some #(when (= :dispatch (:k (first %))) (last %)))
                     vector
                     (cons disp-kw))}
     (str (namespace fq-def)))
    [disp-kw]))

(defmethod analyze-dispatch-type :default [_ctx _fq-def x] x)

(defn analyze-reg-event-fx [ctx expr fq-def]
  (let [[name-expr & body] (next (:children expr))
        [ctx reg-val] (with-context ctx name-expr fq-def)
        body (w/postwalk
              (fn [x]
                (if (coll? x)
                  (analyze-dispatch-type ctx fq-def x)
                  x))
              body)]
    (common/analyze-children ctx (cons reg-val body))))
;;;; Scratch

(comment
  (require '[clj-kondo.core :as clj-kondo])
  (-> (with-in-str "(require '[re-frame.core :as re-frame]) (re-frame/reg-sub ::foo (fn [_]))"
        (clj-kondo/run! {:lang :cljs :lint ["-"] :config {:output {:analysis {:context true
                                                                              :keywords true}}}}))
      :analysis :keywords)

  (-> (with-in-str "(require '[re-frame.core :as re-frame]) (re-frame/reg-sub ::foo (fn [x] (inc x)))"
        (clj-kondo/run! {:lang :cljs :lint ["-"] :config {:output {:analysis {:context true
                                                                              :keywords true}}}}))
      :analysis :var-usages)

  )
