(ns clj-kondo.gusqt-test
  (:require [clojure.test :refer :all]
            [clj-kondo.gusqt :refer :all]))

(deftest as-number-tests
  (testing "as-number"
    (let [expected 0
          actual (as-number '(:this :is :actually :a "list"))]
      (is (= actual expected)))
    (let [expected 0
          actual (as-number [:this :is :actually :a 'vector])]
      (is (= actual expected)))
    (let [expected 0
          actual (as-number "forty-two")]
      (is (= actual expected)))
    (let [expected 0
          actual (as-number :forty-two)]
      (is (= actual expected)))
    (let [expected 0
          actual (as-number 0)]
      (is (= actual expected)))
    (let [expected 42
          actual (as-number 42)]
      (is (= actual expected)))
    (let [expected 42
          actual (as-number (* 6 7))]
      (is (= actual expected)))))

(deftest as-gusqt-tests
  (testing "Conversion vectors of findings maps to GUSQT-style maps"
    (let [expected {}
          actual (as-gusqt [])]
      (is (= actual expected) "if no findings, an empty map"))
    (let [expected {"example.clj"
                    {:lines
                     {0
                      {:clj-kondo
                       {:severity
                        :warning,
                        :file "example.clj",
                        :line 0,
                        :column 0,
                        :test "could not process file"}}}}}
          actual (as-gusqt [{:level :warning
                             :filename "example.clj"
                             :col 0
                             :row 0
                             :message "could not process file"}])]
      (is (= actual expected) "Basic map from single finding"))
    (let [expected {"example.clj"
                    {:lines
                     {0
                      {:clj-kondo
                       {:severity :warning,
                        :file "example.clj",
                        :line 0,
                        :column 0,
                        :test "could not process file"}}}},
                    "missing.clj"
                    {:lines
                     {0
                      {:clj-kondo
                       {:severity :warning,
                        :file "missing.clj",
                        :line 0,
                        :column 0,
                        :test "file does not exist"}}}}}
          actual (as-gusqt [{:level :warning
                             :filename "example.clj"
                             :col 0
                             :row 0
                             :message "could not process file"}
                            {:level :warning
                             :filename "missing.clj"
                             :col 0
                             :row 0
                             :message "file does not exist"}])]
      (is (= actual expected) "Two findings on different files"))))
