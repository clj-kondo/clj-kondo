(ns clj-kondo.impl.analyzer.re-frame
  {:no-doc true}
  (:require
     [clj-kondo.impl.analyzer.common :as common]
     [clojure.walk :as w]))

(defn- assoc-reg-maybe [ctx name-expr fq-def]
  (if-let [kw (:k name-expr)]
    [(assoc name-expr :reg fq-def) (assoc ctx :in-reg {:k kw :reg fq-def})]
    [name-expr ctx]))

(defn analyze-reg [ctx expr fq-def]
  (let [[name-expr & body] (next (:children expr))
        [reg-val ctx] (assoc-reg-maybe ctx name-expr fq-def)]
    (common/analyze-children ctx (cons reg-val body))))

(defn analyze-subscribe [ctx expr]
  (common/analyze-children (assoc ctx :in-subs true) expr))

(defn analyze-dispatch [ctx expr]
  (common/analyze-children (assoc ctx :in-disp true) expr))

(defn analyze-reg-sub [ctx expr fq-def]
  (let [[name-expr & body] (next (:children expr))
        arrow-subs (map last (filter #(= :<- (:k (first %))) (partition-all 2 body)))]
    (if (seq arrow-subs)
      (let [[reg-val ctx] (assoc-reg-maybe ctx name-expr fq-def)]
        (doseq [s arrow-subs]
          (analyze-subscribe ctx [s]))
        (common/analyze-children ctx (cons reg-val (last body))))
      (analyze-reg ctx expr fq-def))))

(defn analyze-reg-event-fx [ctx expr fq-def]
  (let [[name-expr & body] (next (:children expr))
        [reg-val ctx] (assoc-reg-maybe ctx name-expr fq-def)
        body (w/postwalk
              (fn [x]
                (if (and (coll? x) (#{:dispatch :dispatch-n :dispatch-later} (:k (first x))))
                  (let [[disp-kw & disp-coll] x]
                    (analyze-dispatch ctx disp-coll)
                    [disp-kw])
                  x))
              body)]
    (common/analyze-children ctx (cons reg-val body))))
