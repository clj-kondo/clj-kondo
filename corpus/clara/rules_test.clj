(ns clara.rules-test
  (:require [clara.rules :refer [defquery defrule insert!]]
            [clojure.test :as t :refer [deftest is testing]]))

(defquery foo-query
  [?foo-query-value]
  [?query-output <- :foo/bar [{:keys [foo-query-value]}] (= foo-query-value ?foo-query-value)]
  [:test (some? ?query-output)])

(defrule foo-rule
  [?fact <- :foo/bar [{:keys [foo-value]}] (= foo-value ?foo-value)]
  [:test (and (some? ?foo-value)
              (some? ?fact))]
  =>
  (insert! {:fact/type :foo/result
            :fact ?fact
            :result ?foo-value}))

(deftest a-test
  (testing "a-test"
    (is (some? foo-query))
    (is (some? foo-rule))))
