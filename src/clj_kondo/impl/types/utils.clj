(ns clj-kondo.impl.types.utils
  {:no-doc true}
  (:require [clj-kondo.impl.utils :as utils :refer [resolve-call*]]))

(defn truthiness-tag
  "Reduces a tag to a keyword, or to a set of them for a union, so that only the
  truthiness is left. Returns nil when a part of it is unknown. Reads the type
  of a map spec rather than walking its keys."
  [t]
  (cond (keyword? t) (if (= "nilable" (namespace t))
                       ;; nil or the base type, so each half can be judged on
                       ;; its own, e.g. by passed-on-part. hash-set, since the
                       ;; halves coincide for :nilable/nil
                       (hash-set :nil (clojure.core/keyword (name t)))
                       t)
        (set? t) (let [ks (map truthiness-tag t)]
                   (when (every? some? ks)
                     ;; a member can normalize to a union of its own, which
                     ;; belongs in this one rather than nested inside it
                     (into #{} (mapcat #(if (set? %) % [%])) ks)))
        (map? t) (if (identical? :map (:type t))
                   :map
                   (some-> (or (:type t) (:tag t)) truthiness-tag))))

(defn- truthy-keyword?
  "Deliberately narrow, without the type lattice: this decides whether and/or
  stop folding, not whether a linter warns. :true and :truthy come out of the
  folds below, see truthy-part."
  [t]
  (and (keyword? t)
       (not (or (identical? :any t)
                (identical? :nil t)
                (identical? :false t)
                (identical? :boolean t)
                ;; a seqable could be nil, see could-be-relations
                (identical? :seqable t)
                (= "nilable" (namespace t))))))

(defn falsy-keyword? [t]
  (or (identical? :nil t) (identical? :false t)))

(defn- every-tag? [pred t]
  (cond (keyword? t) (pred t)
        (set? t) (and (seq t) (every? pred t))
        :else false))

(defn always-falsy?
  "True when a value of this type is nil or false on every run."
  [x]
  (every-tag? falsy-keyword? (truthiness-tag x)))

(defn never-falsy?
  "True when a value of this type is neither nil nor false."
  [x]
  (every-tag? truthy-keyword? (truthiness-tag x)))

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

(defn- part-of
  "Applies a member refinement over a normalized tag, ::nothing when no member
  is left."
  [t member-part]
  (if (set? t)
    (let [ks (into #{} (comp (map member-part)
                             (remove #(identical? ::nothing %))
                             ;; a member can refine to a union of its own
                             (mapcat #(if (set? %) % [%])))
                   t)]
      (case (count ks)
        0 ::nothing
        1 (first ks)
        ks))
    (member-part t)))

(defn truthy-part
  "The type of `x` when it is truthy, which is what a non final arg of `or`
  contributes. The truthy half of a boolean is :true, of an unknown value
  :truthy: still any type, but neither nil nor false."
  [x]
  (let [t (truthiness-tag x)]
    (if (nil? t)
      :truthy
      (part-of t (fn [t]
                   (cond (falsy-keyword? t) ::nothing
                         (identical? :boolean t) :true
                         (identical? :any t) :truthy
                         :else t))))))

(defn falsy-part
  "The type of `x` when it is falsy, which is what a non final arg of `and`
  contributes. Falsy values are only nil and false, so this stays a plain
  union."
  [x]
  (let [t (truthiness-tag x)]
    (if (nil? t)
      #{:nil :false}
      (part-of t (fn [t]
                   (cond (falsy-keyword? t) t
                         (identical? :boolean t) :false
                         (identical? :any t) #{:nil :false}
                         :else ::nothing))))))

(defn- absorb
  "Drops union members that another member already covers, so the ghost tags do
  not pile up in cached return types. Only wider members absorb: :boolean
  covers :true and :false, :truthy covers every truthy member, :any covers all."
  [t]
  (if-not (set? t)
    t
    ;; a member can be a tag map carrying location info, so judge its tag
    (let [mtag #(if (map? %) (or (:type %) (:tag %)) %)
          tags (into #{} (map mtag) t)]
      (cond (contains? tags :any) :any
            :else
            (let [t (cond->> t
                      (contains? tags :boolean)
                      (remove #(contains? #{:true :false} (mtag %)))
                      (contains? tags :truthy)
                      (remove #(and (truthy-keyword? (mtag %))
                                    (not (identical? :truthy (mtag %))))))
                  t (if (set? t) t (set t))]
              (if (= 1 (count t)) (first t) t))))))

(defn fold-logic
  "The types `and` and `or` can return. An arg matching `stop?` short circuits
  and is the result. A non final arg before that contributes only the part of
  its type that can still be returned, see truthy-part and falsy-part. The
  last arg is always a whole candidate."
  [args stop? part-fn]
  (absorb
   (loop [[a & more] args
          acc #{}]
     (cond (not (seq more)) (union-type acc a)
           (stop? a) (union-type acc a)
           :else (recur more (let [part (part-fn a)]
                               (if (identical? ::nothing part)
                                 acc
                                 (union-type acc part))))))))

(defn resolved-type? [arg-type]
  (or (keyword? arg-type)
      (and (set? arg-type) (every? resolved-type? arg-type))
      (and (map? arg-type) (when-let [t (:type arg-type)]
                             (and (not (:kw-calls arg-type))
                                  (identical? t :map))))))

(defn map-kw-lookup
  "Value tag of `kw-call` in map type `t`: the entry's tag, provably :nil
  when the key is missing from a closed map, nil when missing from an open
  one."
  [t kw-call]
  (if-let [e (find (:val t) kw-call)]
    (:tag (val e))
    (when-not (:open t) :nil)))

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
                         (if (identical? t :map)
                           (if-let [[kw-call & rest-kw-calls] (seq (:kw-calls arg-type))]
                             ;; a key missing from a closed map is provably
                             ;; nil, and any deeper lookup on nil stays nil
                             (let [resolved-tag (map-kw-lookup arg-type kw-call)]
                               (cond
                                 (and rest-kw-calls
                                      (= :map (:type resolved-tag)))
                                 (resolve-arg-type idacs
                                                   (assoc resolved-tag :kw-calls rest-kw-calls)
                                                   seen-calls)

                                 (identical? :nil resolved-tag)
                                 :nil

                                 rest-kw-calls
                                 nil

                                 :else
                                 resolved-tag))
                             t)
                           (resolve-arg-type idacs t seen-calls)))
                       (when-let [call (:call arg-type)]
                         (when-not (contains? seen-calls call)
                           (let [arity (:arity call)]
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
                                 (if-let [kw-calls (:kw-calls call)]
                                   (when (identical? :map (:type resolved-arg-type))
                                     (let [[kw-call & rest-kw-calls] kw-calls
                                           resolved-tag (map-kw-lookup resolved-arg-type kw-call)]
                                       (cond
                                         (and rest-kw-calls
                                              (= :map (:type resolved-tag)))
                                         (resolve-arg-type idacs
                                                           (assoc resolved-tag :kw-calls rest-kw-calls)
                                                           seen-calls)

                                         (identical? :nil resolved-tag)
                                         :nil

                                         rest-kw-calls
                                         nil

                                         :else
                                         ;; resolved-tag may be a :call that still needs resolving
                                         (resolve-arg-type idacs resolved-tag seen-calls))))
                                   (resolve-arg-type idacs resolved-arg-type seen-calls)))))))
                       (when-let [usage (:usage arg-type)]
                         (let [resolved (resolve-call* idacs usage (:resolved-ns usage) (:name usage))]
                           (resolve-arg-type idacs resolved)))
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

(defn strip-positions
  "Strips positions from a map type's :val entries, recursively. A cached
  return's entry positions have no consumer: findings about resolved values
  report at the call site, and serializing them only grows the cache."
  [t]
  (cond (set? t) (into #{} (map strip-positions) t)
        (and (map? t) (:val t))
        (update t :val update-vals
                (fn [e]
                  (let [e (dissoc e :row :col :end-row :end-col)]
                    (if (:tag e)
                      (update e :tag strip-positions)
                      e))))
        :else t))

(defn not-empty-arity [m]
  (when (and m
             (or (:args m)
                 (:ret m)))
    m))

