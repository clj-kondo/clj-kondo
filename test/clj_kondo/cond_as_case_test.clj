(ns clj-kondo.cond-as-case-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :as t :refer [deftest is testing]]))

(def config {:linters {:cond-as-case {:level :warning}}})

(deftest cond-as-case-test
  (testing "cond with = comparisons can be replaced with case"
    (testing "with keyword constants"
      (assert-submaps2
       [{:file "<stdin>"
         :row 1
         :col 1
         :level :warning
         :message "cond can be replaced with case"}]
       (lint! "(cond (= x :a) 1 (= x :b) 2)" config)))

    (testing "with string constants"
      (assert-submaps2
       [{:file "<stdin>"
         :row 1
         :col 1
         :level :warning
         :message "cond can be replaced with case"}]
       (lint! "(cond (= x \"a\") 1 (= x \"b\") 2)" config)))

    (testing "with numeric constants"
      (assert-submaps2
       [{:file "<stdin>"
         :row 1
         :col 1
         :level :warning
         :message "cond can be replaced with case"}]
       (lint! "(cond (= x 1) :a (= x 2) :b)" config)))

    (testing "with vector constants"
      (assert-submaps2
       [{:file "<stdin>"
         :row 1
         :col 1
         :level :warning
         :message "cond can be replaced with case"}]
       (lint! "(cond (= x [:a]) 1 (= x [:b]) 2)" config)))

    (testing "with set constants"
      (assert-submaps2
       [{:file "<stdin>"
         :row 1
         :col 1
         :level :warning
         :message "cond can be replaced with case"}]
       (lint! "(cond (= x #{:a}) 1 (= x #{:b}) 2)" config)))

    (testing "with map constants"
      (assert-submaps2
       [{:file "<stdin>"
         :row 1
         :col 1
         :level :warning
         :message "cond can be replaced with case"}]
       (lint! "(cond (= x {:a 1}) 1 (= x {:b 2}) 2)" config)))

    (testing "with nested constant collections"
      (assert-submaps2
       [{:file "<stdin>"
         :row 1
         :col 1
         :level :warning
         :message "cond can be replaced with case"}]
       (lint! "(cond (= x [[:a]]) 1 (= x [[:b]]) 2)" config))
      (assert-submaps2
       [{:file "<stdin>"
         :row 1
         :col 1
         :level :warning
         :message "cond can be replaced with case"}]
       (lint! "(cond (= x {:a [1 {:b [2]} 3] :c 4}) 1 (= x {:d [5 {:e [6]} 7] :f 8}) 2)" config)))

    (testing "with symbols in nested structures"
      (assert-submaps2
       [{:file "<stdin>"
         :row 1
         :col 1
         :level :warning
         :message "cond can be replaced with case"}]
       (lint! "(cond (= x {:a 'foo :b 'bar}) 1 (= x {:c 'baz :d 'qux}) 2)" config)))

    (testing "with lists containing nested structures"
      (assert-submaps2
       [{:file "<stdin>"
         :row 1
         :col 1
         :level :warning
         :message "cond can be replaced with case"}]
       (lint! "(cond (= x '([:a 1] [:b 2])) 1 (= x '([:c 3] [:d 4])) 2)" config))))

  (testing "no warning when there are hash collisions"
    (is (empty? (lint! "(cond (= x 0) 1 (= x nil) 2)" config)))
    (is (empty? (lint! "(cond (= x 0) 1 (= x 0.0) 2)" config))))

  (testing "no warning with single condition"
    (is (empty? (lint! "(cond (= x :a) 1)" config))))

  (testing "no warning when different variables are used"
    (is (empty? (lint! "(cond (= x :a) 1 (= y :b) 2)" config))))

  (testing "no warning when non-constant expressions"
    (is (empty? (lint! "(cond (= x (foo)) 1 (= x (bar)) 2)" config))))

  (testing "no warning when collections contain non-constants"
    (is (empty? (lint! "(cond (= x [y]) 1 (= x [z]) 2)" config)))
    (is (empty? (lint! "(cond (= x {:a y}) 1 (= x {:b z}) 2)" config)))
    (is (empty? (lint! "(cond (= x {:a [y]}) 1 (= x {:b [z]}) 2)" config)))
    (is (empty? (lint! "(cond (= x {:a [1 {:b y} 3]}) 1 (= x {:c [4 {:d z} 6]}) 2)" config))))

  (testing "no warning by default (linter is off)"
    (is (empty? (lint! "(cond (= x :a) 1 (= x :b) 2)")))))

(deftest condp-equals-as-case-test
  (testing "condp = can be replaced with case"
    (assert-submaps2
     [{:file "<stdin>"
       :row 1
       :col 1
       :level :warning
       :message "condp can be replaced with case"}]
     (lint! "(condp = x :a 1 :b 2)" config))

    (assert-submaps2
     [{:file "<stdin>"
       :row 1
       :col 1
       :level :warning
       :message "condp can be replaced with case"}]
     (lint! "(condp = x :a 1 :b 2 :default)" config)))

  (testing "condp = with strings"
    (assert-submaps2
     [{:file "<stdin>"
       :row 1
       :col 1
       :level :warning
       :message "condp can be replaced with case"}]
     (lint! "(condp = x \"foo\" 1 \"bar\" 2)" config)))

  (testing "condp = with vector constants"
    (assert-submaps2
     [{:file "<stdin>"
       :row 1
       :col 1
       :level :warning
       :message "condp can be replaced with case"}]
     (lint! "(condp = x [:a] 1 [:b] 2)" config)))

  (testing "condp = with map constants"
    (assert-submaps2
     [{:file "<stdin>"
       :row 1
       :col 1
       :level :warning
       :message "condp can be replaced with case"}]
     (lint! "(condp = x {:a 1} 1 {:b 2} 2)" config)))

  (testing "condp = with deeply nested structures"
    (assert-submaps2
     [{:file "<stdin>"
       :row 1
       :col 1
       :level :warning
       :message "condp can be replaced with case"}]
     (lint! "(condp = x {:a [1 {:b [2]} 3] :c 4} 1 {:d [5 {:e [6]} 7] :f 8} 2)" config)))

  (testing "condp = with symbols in nested structures"
    (assert-submaps2
     [{:file "<stdin>"
       :row 1
       :col 1
       :level :warning
       :message "condp can be replaced with case"}]
     (lint! "(condp = x {:a 'foo :b 'bar} 1 {:c 'baz :d 'qux} 2)" config)))

  (testing "condp = with lists containing nested structures"
    (assert-submaps2
     [{:file "<stdin>"
       :row 1
       :col 1
       :level :warning
       :message "condp can be replaced with case"}]
     (lint! "(condp = x '([:a 1] [:b 2]) 1 '([:c 3] [:d 4]) 2)" config)))

  (testing "no warning when there are hash collisions"
    (is (empty? (lint! "(condp = x 0 1 nil 2)" config)))
    (is (empty? (lint! "(condp = x 0 1 0.0 2)" config))))

  (testing "no warning with single test"
    (is (empty? (lint! "(condp = x :a 1)" config))))

  (testing "no warning for non-= predicates (besides contains?)"
    (is (empty? (lint! "(condp some x #{:a} 1 #{:b} 2)" config)))))

(deftest condp-contains-as-case-test
  (testing "condp contains? can be replaced with case"
    (assert-submaps2
     [{:file "<stdin>"
       :row 1
       :col 1
       :level :warning
       :message "condp can be replaced with case"}]
     (lint! "(condp contains? x #{:a} 1 #{:b} 2)" config))

    (assert-submaps2
     [{:file "<stdin>"
       :row 1
       :col 1
       :level :warning
       :message "condp can be replaced with case"}]
     (lint! "(condp contains? x #{:a} 1 #{:b :c} 2)" config))

    (assert-submaps2
     [{:file "<stdin>"
       :row 1
       :col 1
       :level :warning
       :message "condp can be replaced with case"}]
     (lint! "(condp contains? x #{:a} 1 #{:b} 2 :default)" config)))

  (testing "no warning when there are hash collisions"
    (is (empty? (lint! "(condp contains? x #{0} 1 #{nil} 2)" config)))
    (is (empty? (lint! "(condp contains? x #{0} 1 #{0.0} 2)" config))))

  (testing "no warning with single test"
    (is (empty? (lint! "(condp contains? x #{:a} 1)" config))))

  (testing "no warning when sets contain non-constant values"
    (is (empty? (lint! "(condp contains? x #{y} 1 #{z} 2)" config)))))

(deftest cond-as-case-ignore-test
  (testing "can ignore with clj-kondo/ignore"
    (is (empty? (lint! "#_{:clj-kondo/ignore [:cond-as-case]}
                        (cond (= x :a) 1 (= x :b) 2)" config)))
    (is (empty? (lint! "#_{:clj-kondo/ignore [:cond-as-case]}
                        (condp = x :a 1 :b 2)" config)))))

(deftest cond-as-case-config-in-call-test
  (let [config '{:linters {:cond-as-case {:level :warning}}
                 :config-in-call
                 {clojure.core/comment
                  {:linters {:cond-as-case {:level :off}}}}}]
    (testing "can disable with config-in-call for cond"

      (testing "disabled under a specific call"
        (is (empty?
             (lint! "(comment (cond (= x :a) 1 (= x :b) 2))" config)))
        (testing "warning still shows outside the call"
          (assert-submaps2
           [{:row 1 :col 15 :message "cond can be replaced with case"}]
           (lint! "(comment nil) (cond (= x :a) 1 (= x :b) 2)" config)))))

    (testing "can disable with config-in-call for condp"
      (is (empty?
           (lint! "(comment (condp = x :a 1 :b 2))" config)))
      (testing "warning still shows outside the call"
        (assert-submaps2
         [{:row 1 :col 15 :message "condp can be replaced with case"}]
         (lint! "(comment nil) (condp = x :a 1 :b 2)" config)))))

  (testing "can configure via ns metadata"
    (is (empty?
         (lint! "(ns scratch
                   {:clj-kondo/config '{:config-in-call {clojure.core/comment {:linters {:cond-as-case {:level :off}}}}}})
                 (comment (cond (= x :a) 1 (= x :b) 2))"
                {:linters {:cond-as-case {:level :warning}}})))))

(deftest no-warning-with-overridden-equals-test
  (testing "no warning when = is excluded from clojure.core"
    (is (empty? (lint! "(ns my.ns
                          (:refer-clojure :exclude [=]))
                        (defn = [a b] (prn \"custom equals\") false)
                        (cond (= x :a) 1 (= x :b) 2)"
                       config))))

  (testing "warning when explicitly using clojure.core/="
    (assert-submaps2
     [{:file "<stdin>"
       :row 1
       :col 1
       :level :warning
       :message "cond can be replaced with case"}]
     (lint! "(cond (clojure.core/= x :a) 1 (clojure.core/= x :b) 2)" config))))

(comment
  (require '[clojure.test :refer [run-tests]]) 
  (run-tests 'clj-kondo.cond-as-case-test)
  (lint! "(ns scratch
                     {:clj-kondo/config '{:config-in-call {clojure.core/comment {:linters {:cond-as-case {:level :off}}}}}})
                   (comment (cond (= x :a) 1 (= x :b) 2))"
         {:linters {:cond-as-case {:level :warning}}}
         )
  
  (empty? (lint! "#_{:clj-kondo/ignore [:cond-as-case]}
                          (cond (= x :a) 1 (= x :b) 2)"
                 {:linters {:cond-as-case {:level :warning}}}))
  (lint! "(ns my.ns
                            (:refer-clojure :exclude [=]))
                          (defn = [a b] (prn \"custom equals\") false)
                          (cond (= x :a) 1 (= x :b) 2)"
         )
  )