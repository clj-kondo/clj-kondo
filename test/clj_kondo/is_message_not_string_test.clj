(ns clj-kondo.is-message-not-string-test
  (:require
   [clj-kondo.test-utils :as tu :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is testing]]))

(deftest is-message-not-string-test
  (testing "is requires string message in second argument"
    (assert-submaps2
     '({:file "<stdin>",
        :row 2,
        :col 26,
        :level :warning,
        :message "Test assertion message should be a string"})
     (lint! "(require '[clojure.test :refer [is]])
             (is (= 1 1) 42)"
            {:linters {:is-message-not-string {:level :warning}}})))

  (testing "is with non-string message when linter disabled"
    (is (empty? (lint! "(require '[clojure.test :refer [is]])
                         (is (= 1 1) 42)
                         (is (= 2 2) [\"not\" \"equal\"])
                         (is (= 3 3) {:msg \"not equal\"})"
                       {:linters {:is-message-not-string {:level :off}}}))))

  (testing "is with non-string message when linter disabled inline with #_:clj-kondo/ignore"
    (is (empty? (lint! "(require '[clojure.test :refer [is]])
                         (is (= 1 1) #_:clj-kondo/ignore 42)"
                       {:linters {:is-message-not-string {:level :warning}}}))))

  (testing "is with non-string message when linter disabled inline with metadata"
    (is (empty? (lint! "(require '[clojure.test :refer [is]])
                         (is (= 1 1) ^{:clj-kondo/ignore [:is-message-not-string]} 42)"
                       {:linters {:is-message-not-string {:level :warning}}}))))

  (testing "is with non-string message when all linters disabled inline with metadata"
    (is (empty? (lint! "(require '[clojure.test :refer [is]])
                         (is (= 1 1) ^{:clj-kondo/ignore true} 42)"
                       {:linters {:is-message-not-string {:level :warning}}}))))

  (testing "is accepts string type resolved argument"
    (is (empty?
         (lint! "(require '[clojure.test :refer [is]])
             (defn humanize [x] (str x))
             (is (= 1 2) (humanize {:some :data}))"
                {:linters {:is-message-not-string {:level :warning}}})))))
