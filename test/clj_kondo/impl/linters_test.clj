(ns clj-kondo.impl.linters-test
  (:require [clj-kondo.impl.linters :as linters]
            [clojure.test :refer [deftest is testing]]))

(deftest edit-distance-test
  (is (= 0 (linters/edit-distance "name" "name")))
  (is (= 1 (linters/edit-distance "nmae" "name")))
  (is (= 1 (linters/edit-distance "hots" "host")))
  (is (= 1 (linters/edit-distance "join" "join?")))
  (is (= 3 (linters/edit-distance "kitten" "sitting"))))

(deftest similar-key-test
  (testing "a typo suggests the key"
    (is (= :name (linters/similar-key :nmae [:id :name])))
    (is (= :join? (linters/similar-key :join [:join? :port]))))
  (testing "a case mismatch suggests the key"
    (is (= :userId (linters/similar-key :userID [:userId :userName]))))
  (testing "a prefix of a key suggests the key"
    (is (= :sum-lager-ductile
           (linters/similar-key :sum-lager [:days-lager-ductile :sum-handling-ductile :sum-lager-ductile]))))
  (testing "reordered dash-separated words suggest the key"
    (is (= :sum-lager (linters/similar-key :lager-sum [:sum-lager :days]))))
  (testing "a string key is compared by its full text"
    (is (= "name" (linters/similar-key :nmae ["name" "id"]))))
  (testing "a short key, a distant key and a short fragment suggest nothing"
    (is (nil? (linters/similar-key :b [:a])))
    (is (nil? (linters/similar-key :id [:ip :port])))
    (is (nil? (linters/similar-key :mixins [:extends :implements :with])))
    (is (nil? (linters/similar-key :with-mixins [:extends :with])))))
