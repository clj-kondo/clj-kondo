(ns clj-kondo.unknown-map-key-test
  (:require [clj-kondo.impl.linters :as linters]
            [clj-kondo.test-utils :refer [lint! assert-submaps2]]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

(def config {:linters {:unknown-map-key {:level :warning}
                       :type-mismatch {:level :warning}}})

(defn- messages [s]
  (keep (fn [{:keys [row message]}]
          (when (str/starts-with? message "Unknown key")
            [row message]))
        (lint! s config)))

(deftest unknown-map-key-test
  (testing "a lookup on a local closed map warns"
    (is (assert-submaps2
         '({:file "<stdin>" :row 1 :col 20 :level :warning
            :message "Unknown key :nmae, closest match: :name"})
         (lint! "(let [m {:name 1}] (:nmae m))" config))))
  (testing "a lookup on a fn return warns"
    (is (= [[2 "Unknown key :hots, closest match: :host"]]
           (messages "(defn defaults [] {:port 8080 :host \"x\"})
(:hots (defaults))"))))
  (testing "a lookup on a union of closed maps warns"
    (is (= [[2 "Unknown key :eror, closest match: :error"]]
           (messages "(defn result [x] (if x {:ok true :value x} {:ok false :error 1}))
(:eror (result 1))"))))
  (testing "a lookup on a closed map or nil warns"
    (is (= [[1 "Unknown key :b in map with keys :a"]]
           (messages "(:b (when (rand-int 2) {:a 1}))"))))
  (testing "a lookup on a fn return from another namespace warns"
    (is (= [[3 "Unknown key :with-mixins in map with keys :extends, :with"]]
           (messages "(ns a) (defn specs [] {:extends 1 :with 2})
(ns b (:require [a]))
(:with-mixins (a/specs))"))))
  (testing "a :type-mismatch finding at the lookup replaces the warning"
    (is (assert-submaps2
         '({:row 1 :message "Expected: number, received: nil."})
         (lint! "(let [m {:a 1}] (inc (:b m)))" config))))
  (testing "a :constant-condition finding at the lookup replaces the warning"
    (is (assert-submaps2
         '({:row 1 :message "Condition always false"})
         (lint! "(let [m {:a 1}] (when (:b m) 1))"
                (assoc-in config [:linters :constant-condition :level] :warning)))))
  (testing "a default, a present key, an open map, a dynamic var and :off do not warn"
    (is (empty? (messages "(:a {})")))
    (is (empty? (messages "(let [m {:a 1}] (:b m 2))")))
    (is (empty? (messages "(let [m {:a 1}] (:a m))")))
    (is (empty? (messages "(defn f [m] (:b m))")))
    (is (empty? (messages "(let [m (merge {:a 1} (rand))] (:b m))")))
    (is (empty? (messages "(let [m (assoc {:a 1} (rand) 1)] (:b m))")))
    (is (empty? (messages "(def ^:dynamic *opts* {:a 1}) (:b *opts*)")))
    (is (empty? (lint! "(let [m {:a 1}] (:b m))"
                       {:linters {:unknown-map-key {:level :off}}})))))

(deftest edit-distance-test
  (is (= 0 (linters/edit-distance "name" "name")))
  (is (= 1 (linters/edit-distance "nmae" "name")))
  (is (= 1 (linters/edit-distance "hots" "host")))
  (is (= 1 (linters/edit-distance "join" "join?")))
  (is (= 3 (linters/edit-distance "kitten" "sitting"))))
