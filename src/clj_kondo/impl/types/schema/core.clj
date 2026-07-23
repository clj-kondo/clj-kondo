(ns clj-kondo.impl.types.schema.core
  {:no-doc true}
  (:require [clj-kondo.impl.types.utils :as type-utils]))

(defn maybe-tag [t]
  (cond (nil? t) nil
        (keyword? t)
        (cond (identical? :any t) :any
              (= "nilable" (namespace t)) t
              :else (keyword "nilable" (name t)))
        (set? t) (conj t :nil)
        (type-utils/enum-type? t) #{:nil t}
        (and (map? t) (identical? :keys (:op t))) (assoc t :nilable true)
        (type-utils/sequential-type? t) #{:nil t}
        :else nil))

(defn flatten-either-tags [tags]
  (let [flat (into #{} (mapcat #(if (set? %) % [%]) tags))]
    (if (= 1 (count flat))
      (first flat)
      flat)))

(def schema-core
  {'enum {:fn (fn [arg-types]
                (when (seq arg-types)
                  (let [vals (keep (fn [{:keys [tag value]}]
                                     (when (identical? :keyword tag) value))
                                   arg-types)]
                    (when (= (count vals) (count arg-types))
                      {:type :enum :vals (set vals)}))))}
   'maybe {:fn (fn [arg-types]
                 (when-let [t (:tag (first arg-types))]
                   (maybe-tag t)))}
   'either {:fn (fn [arg-types]
                  (when (seq arg-types)
                    (let [tags (map :tag arg-types)]
                      (when (every? some? tags)
                        (flatten-either-tags tags)))))}})
