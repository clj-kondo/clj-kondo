(ns clj-kondo.built-in-specs-test
  "Compares the hand written specs for clojure.core against the return types
  inferred from its sources. A difference means the spec deliberately says
  something else, or one of the two is wrong.

  The differences are written to built_in_spec_differences.txt. When that file
  changes, this test fails: read the diff, and commit it once every new line is
  accounted for."
  (:require
   [clj-kondo.impl.types :as types]
   [clj-kondo.impl.types.clojure.core :refer [clojure-core]]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [cognitect.transit :as transit]))

(def ^:private differences-file
  (io/file "test" "clj_kondo" "built_in_spec_differences.txt"))

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
      {:var nm :arity arity :spec s :inferred i})))

(defn- report [diffs]
  (->> diffs
       (sort-by (juxt (comp str :var) (comp str :arity)))
       (map (fn [{:keys [var arity spec inferred]}]
              (format "%-16s %-9s spec %-24s inferred %s"
                      var arity (pr-str (vec (sort spec))) (pr-str (vec (sort inferred))))))
       (str/join "\n")))

(deftest built-in-specs-test
  (let [diffs (differences)
        actual (str (report diffs) "\n")
        expected (slurp differences-file)]
    (when (not= actual expected)
      (spit differences-file actual))
    (testing "the spec differences are the ones committed, check git diff"
      (let [expected-lines (set (str/split-lines expected))
            actual-lines (set (str/split-lines actual))]
        (is (empty? (remove expected-lines actual-lines)) "new difference")
        (is (empty? (remove actual-lines expected-lines)) "difference is gone")))
    (testing "an inferred type that rejects nil while the spec allows it is unsound"
      (is (empty? (filter #(and (nilable-tags? (:spec %))
                                (not (nilable-tags? (:inferred %))))
                          diffs))))))
