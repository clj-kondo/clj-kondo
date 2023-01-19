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

(deftest lenient-match-test
  (is (types/match? ::unknown-type :string))
  (is (types/match? :string :nilable/any)))

;;;; Scratch

(comment
  (get types/could-be-relations :coll)
  )
