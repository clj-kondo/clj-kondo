(ns clj-kondo.recur-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :as t :refer [deftest is testing]]))

(def linter-config
  {:linters
   {:unexpected-recur {:level :error}}})

(deftest unexpected-recur
  (testing "recur doesn't correspond to function or loop"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 1, :level :error, :message "Unexpected usage of recur."})
     (lint! "(recur)" linter-config)))
  (testing "recur doesn't correspond to function or loop"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 8, :level :error, :message "Recur can only be used in tail position."})
     (lint! "(fn [] (recur) 1)" linter-config)))
  (testing "recur corresponds to function or loop"
    (is (empty? (lint! "(fn [] (recur))" linter-config)))
    (is (empty? (lint! "(loop [] (recur))" linter-config))))
  (testing "recur is in tail position"
    (is (empty? (lint! "(fn [] (if true (recur) 3))" linter-config)))
    (is (empty? (lint! "(fn [x] (case x 1 (recur (inc x)) 2 :the-end))" linter-config)))))

