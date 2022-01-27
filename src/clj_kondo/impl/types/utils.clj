(ns clj-kondo.impl.types.utils
  {:no-doc true}
  (:require [clj-kondo.impl.utils :as utils :refer [resolve-call*]]))

(defn union-type
  ([] #{})
  ([x y]
   (let [ret (cond (or (identical? x :any)
                       (identical? y :any)
                       (identical? (:tag x) :any)
                       (identical? (:tag y) :any)
                       (not x)
                       (not y)) :any
                   (set? x)
                   (if (set? y)
                     (into x y)
                     (conj x y))
                   (set? y)
                   (conj y x)
                   :else (hash-set x y))]
     ;; (prn x '+ y '= ret)
     ret)))

(defn resolved-type? [arg-type]
  (or (keyword? arg-type)
      (and (set? arg-type) (every? resolved-type? arg-type))
      (and (map? arg-type) (when-let [t (:type arg-type)]
                             (and (not (:kw-calls arg-type))
                                  (identical? t :map))))))

(defn resolve-arg-type
  "Resolves arg-type to something which is not a call anymore, i.e. a resolved type or :any."
  ([idacs arg-type] (resolve-arg-type idacs arg-type #{}))
  ([idacs arg-type seen-calls]
   (if (resolved-type? arg-type) arg-type
       (let [ret
             (cond (set? arg-type) (reduce union-type #{} (map #(resolve-arg-type idacs % seen-calls) arg-type))
                   (map? arg-type)
                   (or (when-let [t (:tag arg-type)]
                         (resolve-arg-type
                          idacs
                          (if (map? t)
                            (if (:row t)
                              t
                              ;; should we have added this location info before?
                              (merge t (select-keys arg-type [:row :col :end-row :end-col])))
                            t)
                          seen-calls))
                       (when-let [t (:type arg-type)]
                         (when (identical? t :map)
                           (if-let [[kw-call & rest-kw-calls] (seq (:kw-calls arg-type))]
                             (let [resolved-tag (-> arg-type :val (get kw-call) :tag)]
                               (cond
                                 (and rest-kw-calls
                                      (= (:type resolved-tag) :map))
                                 (resolve-arg-type idacs
                                                   (assoc resolved-tag :kw-calls rest-kw-calls)
                                                   seen-calls)

                                 rest-kw-calls
                                 nil

                                 :else
                                 resolved-tag))
                             arg-type)))
                       (when-let [call (:call arg-type)]
                         (when-not (contains? seen-calls call)
                           (let [arity (:arity call)
                                 kw-calls (:kw-calls call)]
                             (when-let [called-fn (resolve-call* idacs call (:resolved-ns call) (:name call))]
                               (let [arities (:arities called-fn)
                                     tag (or (when-let [v (get arities arity)]
                                               (:ret v))
                                             (when-let [v (get arities :varargs)]
                                               (when (>= arity (:min-arity v))
                                                 (:ret v))))
                                     resolved-arg-type (resolve-arg-type idacs tag (conj seen-calls call))]
                                 ;; (prn arg-type '-> tag)
                                 ;; `kw-calls` exists for dynamic types when using keyword calls.
                                 ;; See `clj-kondo.impl.types/ret-tag-from-call` where
                                 ;; `kw-calls` is introduced.
                                 (if kw-calls
                                   (when (= (:type resolved-arg-type) :map)
                                     (let [[kw-call & rest-kw-calls] kw-calls
                                           resolved-tag (-> resolved-arg-type :val (get kw-call) :tag)]
                                       (cond
                                         (and (seq rest-kw-calls)
                                              (= (:type resolved-tag) :map))
                                         (resolve-arg-type idacs
                                                           (assoc resolved-tag :kw-calls rest-kw-calls)
                                                           seen-calls)

                                         (seq rest-kw-calls)
                                         nil

                                         :else
                                         resolved-tag)))
                                   (resolve-arg-type idacs resolved-arg-type seen-calls)))))))
                       (when-let [op (:op arg-type)]
                         (when (identical? op :keys)
                           {:type :map
                            :val (->> (:req arg-type)
                                      (mapv (fn [[k v]]
                                              [k (merge {:tag (resolve-arg-type idacs v seen-calls)}
                                                        (select-keys arg-type [:row :col :end-row :end-col]))]))
                                      (into {}))}))
                       :any)
                   (nil? arg-type) :any)]
         ;; (prn arg-type '-> ret)
         ret))))

(defn not-empty-arity [m]
  (when (and m
             (or (:args m)
                 (:ret m)))
    m))

(defn resolve-arity-return-types [idacs arities]
  (persistent!
   (reduce-kv
    (fn [m arity v]
      (let [new-v (if-let [ret (:ret v)]
                    (let [t (resolve-arg-type idacs ret)]
                      (if (identical? t :any)
                        (not-empty-arity (dissoc v :ret))
                        (assoc v :ret t)))
                    (not-empty-arity v))]
        (if new-v
          (assoc! m arity new-v)
          (dissoc! m arity))))
    (transient {})
    arities)))

(defn resolve-return-types [idacs ns-data]
  (persistent!
   (reduce-kv
    (fn [m k v]
      (assoc! m k (if-let [arities (:arities v)]
                    (let [new-arities (not-empty (resolve-arity-return-types idacs arities))]
                      (if new-arities
                        (assoc v :arities new-arities)
                        (dissoc v :arities)))
                    v)))
    (transient {})
    ns-data)))
