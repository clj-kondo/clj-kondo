(ns clj-kondo.impl.types-test
  (:require
   [clj-kondo.impl.types :as types]
   [clj-kondo.impl.types.utils :as types-utils]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest x-is-y-and-y-is-z-implies-x-is-z
  (doseq [[x ys] types/is-a-relations
          y ys
          z (get types/is-a-relations y)]
    (testing (format "%s is a %s and %s is a %s => %s is a %s"
                     x y y z x z)
      (is (contains? ys z)))))

;; NOTE: could be relations are not transitive: :coll could be an :ifn and :ifn
;; could be a :keyword, but a :coll could not be a keyword

(deftest x-is-y-implies-y-could-be-x
  (doseq [[t ares] types/is-a-relations
          is-a ares]
    (testing (format "%s is a %s => %s could be a %s"
                     t is-a is-a t)
      (is (contains? (get types/could-be-relations is-a) t)))))

(deftest x-could-be-y-and-y-is-z-implies-x-could-also-be-z
  (doseq [[x ys] types/could-be-relations
          y ys
          :let [is-a (get types/is-a-relations x)
                zs (get types/is-a-relations y)]]
    (doseq [z zs
            :when (not (or
                        (contains? is-a z)
                        (= x z)))]
      (testing (format "%s could be %s, %s is %s, implies %s could also be a %s"
                       x y y z x z)
        (is (contains? ys z))))))

(deftest all-types-have-labels
  (let [all-types (concat (keys types/is-a-relations)
                          (keys types/could-be-relations)
                          types/misc-types)]
    (doseq [t all-types]
      (is (types/label t)))))

(deftest union-type-test
  (testing "fix for #1023"
    (is (= #{} (types-utils/union-type)))))

(deftest intersect-test
  (testing ":any constrains nothing, on either side"
    (is (= :long (types/intersect :any :long)))
    (is (= :long (types/intersect :long :any)))
    (is (= :any (types/intersect :any :any))))
  (testing "conflicting types intersect to nil"
    (is (nil? (types/intersect :string :number))))
  (let [kts (conj (vec types/known-types) :any)
        norm (fn [x] (if (set? x) x (when x #{x})))]
    (doseq [a kts b kts]
      (testing (format "intersect is commutative for %s and %s" a b)
        (is (= (norm (types/intersect a b)) (norm (types/intersect b a)))))
      (when (types/is-a? a b)
        (testing (format "%s is a %s => intersection is %s" a b a)
          (is (= (norm a) (norm (types/intersect a b)))))))
    (doseq [a kts]
      (testing (format "intersect is idempotent for %s" a)
        (is (= (norm a) (norm (types/intersect a a))))))))

(deftest match-test
  (is (not (types/match? :var :number))))

(deftest lenient-match-test
  (is (types/match? ::unknown-type :string))
  (is (types/match? :string :nilable/any)))

;;;; Scratch

(comment
  (get types/could-be-relations :coll)
  (require '[clj-kondo.core] :reload-all)
  (-> (with-in-str
        "(get nil [])"
        (clj-kondo.core/run! {:lint ["-"]}))
      :findings)
  )
