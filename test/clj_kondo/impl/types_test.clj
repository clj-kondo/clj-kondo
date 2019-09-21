(ns clj-kondo.impl.types-test
  (:require
   ;; [clj-kondo.impl.clojure.spec.alpha :as s]
   [clj-kondo.impl.types :as types]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest all-what-is-could-be
  (is (empty?
       (distinct (for [[k vs] types/is-a-relations
                       v vs
                       :let [could-bes (get types/could-be-relations v)]
                       :when (not (contains? could-bes k))]
                   k)))))

(deftest all-what-could-be-is
  (is (empty?
       (distinct (for [[k vs] types/could-be-relations
                       v vs
                       :let [are (get types/is-a-relations v)]
                       :when (not (contains? are k))]
                   k)))))

(deftest all-types-have-labels
  (let [all-types (concat (keys types/is-a-relations)
                          (keys types/could-be-relations)
                          types/misc-types)]
    (doseq [t all-types]
      (is (types/label t)))))
