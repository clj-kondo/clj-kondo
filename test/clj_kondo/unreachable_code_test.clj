(ns clj-kondo.unreachable-code-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps2]]
   [clojure.test :as t :refer [deftest is testing]]))

(def config
  {:linters {:type-mismatch {:level :error}
             :unreachable-code {:level :warning}}})

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
  (assert-submaps2
   [{:file "<stdin>"
     :row 1
     :col 5
     :level :warning
     :message "Condition always true"}]
   (lint! "(if :a 1 2)" config))
  (assert-submaps2
   [{:file "<stdin>"
     :row 1
     :col 12
     :level :warning
     :message "Condition always true"}]
   (lint! "(if-let [a :a] 1 2)" config))
  (assert-submaps2
   [{:file "<stdin>"
     :row 1
     :col 7
     :level :warning
     :message "Condition always true"}]
   (lint! "(when :a 1)" config)))

(deftest constants-test
  (assert-submaps2
   [{:file "<stdin>"
     :row 3
     :col 16
     :level :warning
     :message "Condition always true"}
    {:file "<stdin>"
     :row 4
     :col 16
     :level :warning
     :message "Condition always false"}]
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
          config))
  (assert-submaps2
   []
   (lint! "(cond-> {} :always (assoc :hello :goodbye))"
          config))
  (assert-submaps2
   [{:file "<stdin>"
     :row 1
     :col 12
     :level :warning
     :message "Condition always true"}]
   (lint! "(cond-> {} :true (assoc :hello :goodbye))"
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

(deftest are-test
  (assert-submaps2 []
                   (lint! "(require '[clojure.test :refer [are]])
(are [exp time-style]
    (= exp (when time-style true))
  :dude :dude
  true nil)"
                          config)))

(deftest is-test
  (testing "constants"
    (assert-submaps2
     [{:file "<stdin>",
       :row 1,
       :col 43,
       :level :warning,
       :message "Condition always true"}]
     (lint! "(require '[clojure.test :refer [is]]) (is 42)"
            config))
    (assert-submaps2
     [{:file "<stdin>",
       :row 1,
       :col 43,
       :level :warning,
       :message "Condition always true"}]
     (lint! "(require '[clojure.test :refer [is]]) (is \"hello\")"
            config))
    (assert-submaps2
     [{:file "<stdin>",
       :row 1,
       :col 43,
       :level :warning,
       :message "Condition always true"}]
     (lint! "(require '[clojure.test :refer [is]]) (is :keyword)"
            config)))
  (testing "functions"
    (assert-submaps2
     [{:file "<stdin>",
       :row 1,
       :col 43,
       :level :warning,
       :message "Condition always true"}]
     (lint! "(require '[clojure.test :refer [is]]) (is inc)"
            config))
    (assert-submaps2
     [{:file "<stdin>",
       :row 1,
       :col 43,
       :level :warning,
       :message "Condition always true"}]
     (lint! "(require '[clojure.test :refer [is]]) (is odd?)"
            config)))
  (testing "var"
    (assert-submaps2
     [{:file "<stdin>",
       :row 1,
       :col 43,
       :level :warning,
       :message "Condition always true"}]
     (lint! "(require '[clojure.test :refer [is]]) (is #'inc)"
            config)))
  (testing "valid calls - no warnings"
    (is (empty?
         (lint! "(require '[clojure.test :refer [is]]) (is (odd? 3))"
                config)))
    (is (empty?
         (lint! "(require '[clojure.test :refer [is]]) (is (some? nil))"
                config)))
    (is (empty?
         (lint! "(require '[clojure.test :refer [is]]) (is true)"
                config))))
  (testing "cljs.test with function"
    (assert-submaps2
     [{:file "<stdin>",
       :row 1,
       :col 40,
       :level :warning,
       :message "Condition always true"}]
     (lint! "(require '[cljs.test :refer [is]]) (is inc)"
            {:linters {:unreachable-code {:level :warning}}
             :lang :cljs}))))

(deftest condition-always-false-test
  (testing "nil literal in condition position"
    (assert-submaps2
     [{:file "<stdin>"
       :row 1
       :col 7
       :level :warning
       :message "Condition always false"}]
     (lint! "(when nil 1)" config))
    (assert-submaps2
     [{:file "<stdin>"
       :row 1
       :col 5
       :level :warning
       :message "Condition always false"}]
     (lint! "(if nil 1 2)" config)))
  (testing "a key lookup that is provably nil"
    (assert-submaps2
     [{:file "<stdin>"
       :row 1
       :col 7
       :level :warning
       :message "Condition always false"}]
     (lint! "(when (:k {}) 1)" config))
    (assert-submaps2
     [{:file "<stdin>"
       :row 1
       :col 24
       :level :warning
       :message "Condition always false"}]
     (lint! "(let [n (:k {})] (when n 1))" config)))
  (testing "false is exempt, an intentional dev toggle"
    (is (empty? (lint! "(when false 1)" config)))
    (is (empty? (lint! "(let [flag false] (when flag 1))" config))))
  (testing "unknown or nilable conditions are fine"
    (is (empty? (lint! "(defn f [x] (when x 1))" config)))
    (is (empty? (lint! "(when (seq [1 2 3]) 1)" config)))))

(deftest inferred-return-type-test
  (let [config {:linters {:type-mismatch {:level :error}
                          :unreachable-code {:level :warning}}}]
    (testing "the return type of a var is resolved after every namespace is analyzed"
      (assert-submaps2
       '({:row 1 :col 43 :message "Condition always false"})
       (lint! "(ns foo) (defn f [] nil) (defn g [] (when (f) 1))" config))
      (assert-submaps2
       '({:row 1 :col 46 :message "Condition always true"})
       (lint! "(ns foo) (defn f [] {:a 1}) (defn g [] (when (f) 1))" config))
      (assert-submaps2
       '({:row 1 :col 55 :message "Condition always true"})
       (lint! "(ns foo) (defn f [] (filter odd? [1])) (defn g [] (if (f) 1 2))" config)))
    (testing "a nilable or boolean return is fine"
      (is (empty? (lint! "(ns foo) (defn f [] (seq [])) (defn g [] (when (f) 1))" config)))
      (is (empty? (lint! "(ns foo) (defn f [] (odd? 1)) (defn g [] (when (f) 1))" config))))
    (testing "a built-in call reports once, not once per phase"
      (assert-submaps2
       '({:row 1 :message "Condition always true"})
       (lint! "(defn g [coll] (when (filter odd? coll) 1))" config)))
    (testing "a return type inferred from a built-in spec keeps its nil case"
      (is (empty? (lint! "(ns foo) (defn dt? [s] (re-matches #\"x\" s)) (defn g [s] (if (dt? s) 1 2))"
                         config)))
      (is (empty? (lint! "(ns foo) (defn f [s] (re-find #\"x\" s)) (defn g [s] (when (f s) 1))"
                         config))))
    (testing "a var with a built-in spec is left to the analysis phase"
      (is (empty? (lint! "(when (re-matches #\"x\" \"y\") 1)" config))))
    (testing "a lint-as'ed conditional does not check its condition"
      (is (empty? (lint! "(ns foo) (defn t [] {:a 1}) (defmacro m [c b] c) (defn g [] (m (t) 1))"
                         (assoc config :lint-as '{foo/m clojure.core/if})))))))
