(ns clj-kondo.built-in-specs-test
  "Compares the hand written specs for clojure.core against the return types
  inferred from its sources. A difference means one of the two is wrong, so new
  ones are reported. Known differences live in built_in_spec_differences.edn,
  regenerate it with CLJ_KONDO_SPEC_DIFF_UPDATE=true."
  (:require
   [clj-kondo.impl.types :as types]
   [clj-kondo.impl.types.clojure.core :refer [clojure-core]]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [clojure.test :refer [deftest is testing]]
   [cognitect.transit :as transit]))

(def ^:private baseline-file
  (io/file "test" "clj_kondo" "built_in_spec_differences.edn"))

(defn- inferred-core []
  (with-open [is (io/input-stream
                  (io/resource "clj_kondo/impl/cache/built_in/clj/clojure.core.transit.json"))]
    (transit/read (transit/reader is :json))))

(defn- tag-set
  "Normalizes a ret tag to a set of keywords, nil when not comparable."
  [t]
  (cond (keyword? t) #{t}
        (set? t) (when (every? keyword? t) t)
        (and (map? t) (:type t)) (tag-set (:type t))
        :else nil))

(defn- nilable-tags? [ts]
  (boolean (some #(or (identical? :nil %) (types/nilable? %) (identical? :any %)) ts)))

(defn- classify [spec-ret inferred-ret]
  (cond (and (nilable-tags? inferred-ret) (not (nilable-tags? spec-ret))) :spec-misses-nil
        (and (nilable-tags? spec-ret) (not (nilable-tags? inferred-ret))) :inferred-misses-nil
        :else :other))

(defn differences
  "Every var and arity whose hand written ret differs from the inferred one."
  []
  (let [inferred (inferred-core)]
    (into #{}
          (for [[nm spec] clojure-core
                :let [spec-arities (:arities spec)
                      inf-arities (:arities (get inferred nm))]
                :when (and spec-arities inf-arities)
                [arity sa] spec-arities
                :let [ia (get inf-arities arity)]
                :when ia
                :let [s (tag-set (:ret sa))
                      i (tag-set (:ret ia))]
                :when (and s i (not= s i))]
            [nm arity (classify s i) s i]))))

(deftest built-in-specs-test
  (let [actual (differences)]
    (when (= "true" (System/getenv "CLJ_KONDO_SPEC_DIFF_UPDATE"))
      (spit baseline-file (with-out-str (pp/pprint (vec (sort-by pr-str actual))))))
    (let [expected (set (edn/read-string (slurp baseline-file)))]
      (testing "no new difference between a hand written spec and the inferred type"
        (is (empty? (remove expected actual))))
      (testing "known differences that are gone can be dropped from the baseline"
        (is (empty? (remove actual expected))))
      (testing "an inferred type that rejects nil while the spec allows it makes the linters unsound"
        (is (empty? (filter #(identical? :inferred-misses-nil (nth % 2)) actual)))))))
