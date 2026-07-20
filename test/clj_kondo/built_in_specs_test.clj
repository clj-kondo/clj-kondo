(ns clj-kondo.built-in-specs-test
  "Compares the hand written specs for clojure.core against the return types
  inferred from its sources. A difference means the spec deliberately says
  something else, or one of the two is wrong.

  built_in_spec_differences.edn groups the known ones by reason. Regenerate it
  with CLJ_KONDO_SPEC_DIFF_UPDATE=true, which keeps those groups and puts
  anything new under :unclassified for you to move into a group."
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
            [nm arity s i]))))

(defn- inferred-misses-nil?
  "The spec allows nil where the inferred type does not. Such an inferred type
  makes anything built on it unsound, see the re-find case in 2026.05."
  [[_ _ spec-ret inferred-ret]]
  (and (nilable-tags? spec-ret) (not (nilable-tags? inferred-ret))))

(deftest built-in-specs-test
  (let [actual (differences)
        baseline (edn/read-string (slurp baseline-file))
        known (into #{} (mapcat val) baseline)]
    (when (= "true" (System/getenv "CLJ_KONDO_SPEC_DIFF_UPDATE"))
      (let [kept (update-vals baseline #(vec (sort-by pr-str (filter actual %))))
            new-ones (vec (sort-by pr-str (remove known actual)))]
        (spit baseline-file
              (with-out-str
                (pp/pprint (cond-> kept
                             (seq new-ones) (assoc :unclassified new-ones)))))))
    (let [baseline (edn/read-string (slurp baseline-file))
          known (into #{} (mapcat val) baseline)]
      (testing "a new difference needs a reason, put it in a group in the edn"
        (is (empty? (remove known actual))))
      (testing "a difference that is gone can be dropped from the edn"
        (is (empty? (remove actual known))))
      (testing "every known difference is grouped by reason"
        (is (empty? (:unclassified baseline))))
      (testing "an inferred type that rejects nil while the spec allows it is unsound"
        (is (empty? (filter inferred-misses-nil? actual)))))))
