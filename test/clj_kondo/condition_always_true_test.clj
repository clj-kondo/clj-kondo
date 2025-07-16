(ns clj-kondo.condition-always-true-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps2]]
   [clojure.test :as t :refer [deftest is testing]]))

(def config
  {:linters {:type-mismatch {:level :error}
             :condition-always-true {:level :warning
                                     :allow-keywords true}}})

(deftest condition-always-true-test
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 20, :level :warning, :message "Condition always true"}
     {:file "<stdin>",
      :row 1,
      :col 35,
      :level :warning,
      :message "Condition always true"})
   (lint! "(defn foo [x] [(if inc x 2) (when inc 2)])"
          config))
  (is (empty?
       (lint! "(defn foo [x] (if x inc 2))
               (defn bar [x] (if x 2 inc))"
              config)))
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 7,
     :level :warning,
     :message "Condition always true"}]
   (lint! "(when #'inc 2)"
          config))
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 9,
     :level :warning,
     :message "Condition always true"}]
   (lint! "(if-not odd? 1 2)"
          config))
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 12,
     :level :warning,
     :message "Condition always true"}]
   (lint! "(if-let [a odd?] 1 2)"
          config)))

(deftest calls-test
  (assert-submaps2
   []
   (lint! "(if (meta name) 1 2)"
          config))
  (assert-submaps2
   []
   (lint! "(if-let [n (namespace :hello)] 1 2)"
          config))
  (assert-submaps2
   []
   (lint! "(let [ns (:ns info) goog? (when ns 1 2)] 1 2)"
          config)))

(deftest symbols-test
  (assert-submaps2
   []
   (lint! "(ns foo (:require [blah :refer [func]])) (let [a func] (if a 1 2))"
          config))
  (assert-submaps2
   []
   (lint! "(ns foo (:require [blah :refer [func]])) (if-let [a func] 1 2)"
          config))
  (assert-submaps2
   []
   (lint! "(when a 1)"
          config)))

(deftest keywords-test
  (testing ":allow-keywords true"
    (assert-submaps2
     []
     (lint! "(if :a 1 2)" config))
    (assert-submaps2
     []
     (lint! "(if-let [a :a] 1 2)" config))
    (assert-submaps2
     []
     (lint! "(when :a 1)" config)))
  (testing ":allow-keywords false"
    (let [config (assoc-in config [:linters :condition-always-true :allow-keywords] false)]
      (assert-submaps2
       [{:file "<stdin>"
         :row 1
         :col 5
         :level :warning
         :message "Condition always true"}]
       (lint! "(if :a 1 2)"
              config))
      (assert-submaps2
       [{:file "<stdin>"
         :row 1
         :col 12
         :level :warning
         :message "Condition always true"}]
       (lint! "(if-let [a :a] 1 2)"
              config))
      (assert-submaps2
       [{:file "<stdin>"
         :row 1
         :col 7
         :level :warning
         :message "Condition always true"}]
       (lint! "(when :a 1)"
              config))
      (assert-submaps2
       [{:file "<stdin>"
         :row 1
         :col 12
         :level :warning
         :message "Condition always true"}]
       (lint! "(cond-> {} :always (assoc :hello :goodbye))"
              config)))))

(deftest constants-test
  (assert-submaps2
   [{:file "<stdin>"
     :row 3
     :col 16
     :level :warning
     :message "Condition always true"}]
   (lint! "(let [a 4 b nil]
             (cond-> {}
               a (assoc :a a)
               b (assoc :b b)))"
          config)))

(deftest cond-test
  (assert-submaps2
   [{:file "<stdin>"
     :row 1
     :col 7
     :level :warning
     :message "use :else as the catch-all test expression in cond"}]
   (lint! "(cond true (assoc foo :hello :goodbye))"
          config))
  (assert-submaps2
   [{:file "<stdin>"
     :row 1
     :col 7
     :level :warning
     :message "use :else as the catch-all test expression in cond"}]
   (lint! "(cond :true (assoc foo :hello :goodbye))"
          config)))

(deftest cond-arrow-test
  (assert-submaps2
   []
   (lint! "(cond-> {} true (assoc :hello :goodbye))"
          config)))

(deftest lazy-seqs-test
  (testing "unrealized"
    (assert-submaps2
     [{:file "<stdin>"
       :row 1
       :col 5
       :level :warning
       :message "Condition always true"}]
     (lint! "(if (filter identity ()) 1 2)"
            config))
    (assert-submaps2
     [{:file "<stdin>"
       :row 1
       :col 12
       :level :warning
       :message "Condition always true"}]
     (lint! "(if-let [a (take 5 ())] 1 2)"
            config))
    (assert-submaps2
     [{:file "<stdin>"
       :row 1
       :col 7
       :level :warning
       :message "Condition always true"}]
     (lint! "(when (rest nil) 1)"
            config)))
  (testing "calling seq"
    (assert-submaps2
     []
     (lint! "(if (seq (filter identity ())) 1 2)"
            config))
    (assert-submaps2
     []
     (lint! "(let [subs (map identity [])] (if-let [[[ic itx {style :a} :as h] & t] (seq subs)] 1 2))"
            config))))

(deftest quoted-object-test
  (assert-submaps2
   [{:file "<stdin>"
     :row 1
     :col 18
     :level :warning
     :message "Condition always true"}]
   (lint! "(when-let [[a b] '(1 2 3)] a 2)"
          config)))

(deftest binding-test
  (assert-submaps2
   [{:file "<stdin>"
     :row 1
     :col 22
     :level :warning
     :message "Condition always true"}]
   (lint! "(if-let [{:keys [a]} {:a :b}] a 2)"
          config))
  (assert-submaps2
   [{:file "<stdin>"
     :row 1
     :col 18
     :level :warning
     :message "Condition always true"}]
   (lint! "(when-let [[a b] '(1 2 3)] a 2)"
          config))
  (assert-submaps2
   [{:file "<stdin>"
     :row 1
     :col 18
     :level :warning
     :message "Condition always true"}]
   (lint! "(when-let [[a b] [(foo) (bar)]] a 2)"
          config)))
