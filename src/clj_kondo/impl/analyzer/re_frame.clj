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

(defn analyze-reg
  "Analyzes re-frame event registrations that do not need custom analysis.
  Makes sure that the body of the event registration can refer back to the event id using
  the analysis context."
  [ctx expr fq-def]
  (let [[name-expr & body] (next (:children expr))
        [ctx reg-val] (with-context ctx name-expr fq-def)]
    (common/analyze-children ctx (cons reg-val body))))

(defn analyze-subscribe
  "Analyzes re-frame subscribe call.
  Marks the query id, the first element of the parameter vector as a subscription reference so
  it can be tracked back to the subscription. Marker is `[:context :re-frame.core :subscription-ref]`.
  https://day8.github.io/re-frame/api-re-frame.core/#subscribe"
  [ctx expr ns]
  (let [kns (keyword ns)
        [farg & args :as children] (next (:children expr))]
    (if (and farg (identical? :vector (utils/tag farg)))
      (let [[subscription-id & subscription-params] (:children farg)]
        (common/analyze-children (assoc-in ctx [:context kns :subscription-ref] true) [subscription-id])
        (when subscription-params
          (common/analyze-children ctx subscription-params))
        (when args
          (common/analyze-children ctx args)))
      (common/analyze-children ctx children))))

(defn analyze-inject-cofx
  "Analyzes re-frame co effect injection.
  Marks the co effect id, the first parameter as a co effect reference so it can be tracked back
  to the co effect registration. Marker is `[:context :re-frame.core :cofx-ref]`.
  https://day8.github.io/re-frame/api-re-frame.core/#inject-cofx"
  [ctx expr ns]
  (let [kns (keyword ns)
        [cofx-id & cofx-params] (next (:children expr))]
    (common/analyze-children (assoc-in ctx [:context kns :cofx-ref] true) [cofx-id])
    (when cofx-params
      (common/analyze-children ctx cofx-params))))

(defn- analyze-dispatch-event-id
  "Marks the event id, the first element of the parameter vector as an event reference so
  it can be tracked back to the event registration. Marker is `[:context :re-frame.core :event-ref]`.
  https://day8.github.io/re-frame/api-re-frame.core/#dispatch"
  [ctx expr ns]
  (let [kns (keyword ns)
        [farg & _args :as _children] (next (:children expr))]
    (when (and farg (identical? :vector (utils/tag farg)))
      (let [[event-id & _event-params] (:children farg)]
        (when (utils/keyword-node? event-id)
          (common/analyze-children (assoc-in ctx [:context kns :event-ref] true) [event-id]))))))

(defn analyze-dispatch
  "Analyzes re-frame dispatch call.
  Uses [[analyze-dispatch-event-id]] to analyze the event id. And analyzes the rest of the dispatch call
  as usual."
  [ctx expr ns]
  (analyze-dispatch-event-id ctx expr ns)
  (common/analyze-children ctx (next (:children expr))))

(defn analyze-reg-sub
  "Analyzes re-frame subscription registration.
  Apart from `analyze-reg` functionality it takes care of the analysis of `:<-` syntactic sugar
  referencing other subscriptions so those can be tracked. Signal function does not need
  special analysis as it uses `subscribe` calls which `analyze-subscribe` takes care of.
  See also [[analyze-reg]], [[analyze-subscribe]]
  https://day8.github.io/re-frame/api-re-frame.core/#reg-sub"
  [ctx expr fq-def]
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
  (analyze-dispatch-event-id ctx {:children x} (str (namespace fq-def)))
  x)

(defmethod analyze-dispatch-type :dispatch-n [ctx fq-def x]
  (let [disp-kw (:k (first x))
        second-x (second x)]
    (when (and second-x (identical? :vector (utils/tag second-x)))
      (doseq [dispatch-vector (:children (second x))]
        (analyze-dispatch-event-id ctx {:children (cons disp-kw [dispatch-vector])} (str (namespace fq-def)))))
    x))

(defmethod analyze-dispatch-type :dispatch-later [ctx fq-def x]
  (let [disp-kw (:k (first x))]
    (analyze-dispatch-event-id
     ctx
     {:children (->> (last x)
                     :children
                     (partition-all 2)
                     (some #(when (= :dispatch (:k (first %))) (last %)))
                     vector
                     (cons disp-kw))}
     (str (namespace fq-def)))
    x))

(defmethod analyze-dispatch-type :default [_ctx _fq-def x] x)

(defn analyze-reg-event-fx
  "Analyzes re-frame event handler registration.
  Apart from `analyze-reg` functionality it looks for dispatch keyword variations `:dispatch`, `:dispatch-n`,
  `:dispatch-later` and marks the event ids in their parameters so those can be tracked.
  See also [[analyze-reg]], [[analyze-dispatch]]
  https://day8.github.io/re-frame/api-re-frame.core/#reg-event-fx"
  [ctx expr fq-def]
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
        (clj-kondo/run! {:lang :cljs :lint ["-"] :config {:analysis {:context true
                                                                     :keywords true}}}))
      :analysis :keywords)

  (-> (with-in-str "(require '[re-frame.core :as re-frame]) (re-frame/reg-sub ::foo (fn [x] (inc x)))"
        (clj-kondo/run! {:lang :cljs :lint ["-"] :config {:analysis {:context true
                                                                     :keywords true}}}))
      :analysis :var-usages))
