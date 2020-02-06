(ns clj-kondo.impl.types.utils
  {:no-doc true}
  (:require [clj-kondo.impl.utils :refer [resolve-call*]]))

(defn call [x]
  (when (:call x)
    x))

(defn union-type
  [x y]
  (let [ret (cond (or (identical? x :any)
                      (identical? y :any)
                      (identical? (:tag x) :any)
                      (identical? (:tag y) :any)
                      (not x)
                      (not y)) nil
                  (set? x)
                  (if (set? y)
                    (into x y)
                    (conj x y))
                  :else (hash-set x y))]
    ;; (prn x '+ y '= ret)
    ret))

(defn resolved-type? [t]
  (or (keyword? t)
      (and (set? t) (every? resolved-type? t))
      (and (map? t) (when-let [t (:type t)]
                      (identical? t :map)))))

(defn resolve-arg-type
  ([idacs arg-type] (resolve-arg-type idacs arg-type #{}))
  ([idacs arg-type seen-calls]
   (if (resolved-type? arg-type) arg-type
       (let [ret
             (cond (set? arg-type) (reduce union-type (map #(resolve-arg-type idacs % seen-calls) arg-type))
                   (map? arg-type)
                   (or (when-let [t (:tag arg-type)] (resolve-arg-type idacs t seen-calls))
                       (when-let [t (:type arg-type)]
                         (when (identical? t :map) arg-type))
                       (if-let [call (:call arg-type)]
                         (when-not (contains? seen-calls call)
                           (let [arity (:arity call)]
                             (when-let [called-fn (resolve-call* idacs call (:resolved-ns call) (:name call))]
                               (let [arities (:arities called-fn)
                                     tag (or (when-let [v (get arities arity)]
                                               (:ret v))
                                             (when-let [v (get arities :varargs)]
                                               (when (>= arity (:min-arity v))
                                                 (:ret v))))]
                                 ;; (prn arg-type '-> tag)
                                 (resolve-arg-type idacs tag (conj seen-calls call))))))
                         :any)
                       :any)
                   (nil? arg-type) :any)]
         ;; (prn arg-type '-> ret)
         ret))))
