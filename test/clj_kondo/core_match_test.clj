(ns clj-kondo.core-match-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps lint!]]
   [clojure.test :as t :refer [deftest is testing]]
   [missing.test.assertions]))

(defn lint!! [s]
  (lint! (format "(require '[clojure.core.match :refer [match]])
%s
" s)
         {:linters {:unused-binding {:level :warning}
                    :unresolved-symbol {:level :error}}}))

(deftest core-match-test
  (assert-submaps
   '({:file "<stdin>", :row 4, :col 8, :level :warning, :message "unused binding a"})
   (lint!! "
(match [1 2 3]
  [1 2 a] :foo)
"))
  (testing "cljs"
    (assert-submaps
     '({:file "<stdin>", :row 3, :col 13, :level :warning, :message "unused binding a"})
     (lint! "(ns foo (:require [cljs.core.match :refer-macros [match]]))
(match [1 2 3]
       [1 2 a] :foo)"
            {:linters {:unused-binding {:level :warning}
                       :unresolved-symbol {:level :error}}})))
  (assert-submaps
   '({:file "<stdin>", :row 5, :col 15, :level :warning, :message "unused binding b"})
   (lint!! "
(let [x {:a 1 :b 1}]
  (match [x]
         [{:a b :b 2}] :a0
         [{:a 1 :b 1}] :a1
         [{:c 3 :d x :e 4}] :a2
         :else nil))"))
  (is (empty? (lint!! "

(match [1 2 3]
  [1 2 _] :foo)
")))
  (is (empty? (lint!! "

(match [1 2 3]
  [1 2 a] a)
")))
  (is (empty? (lint!! "

(match [1 2 [3 4]]
  [1 2 [a b]] [a b])
")))
  (is (empty? (lint!! "

(match [1 2 [3 4]]
  [1 2 [a b]] [a b])
")))
  (testing "& rest pattern"
    (is (empty? (lint!! "
(let [x [1 2 3]]
  (match x
         [1 & rs] rs))
"))))
  (testing "& rest pattern"
    (is (empty? (lint!! "
(let [x [1 2 nil nil nil]]
  (match [x]
    [([1] :seq)] :a0
    [([1 2] :seq)] :a1
    [([1 2 nil nil nil] :seq)] :a2
    :else nil))
;=> :a2
"))))
  (testing "nested seqs"
    (is (empty? (lint!! "
(let [x '((1 2))]
  (match x
         ([([1 & r] :seq)] :seq) [:a1 r]))
"))))
  (testing "symbolic clause"
    (is (empty? (lint!! "
(let [x '((1 2))]
  (match x
         y y))
"))))

  (testing "map pattern"
    (is (empty? (lint!! "
(let [x {:a 1 :b 1}]
  (match [x]
         [{:a _ :b 2}] :a0
         [{:a 1 :b 1}] :a1
         [{:c 3 :d _ :e 4}] :a2
         :else nil))
"))))
  (testing ":or pattern"
    (is (empty? (lint!! "
(let [x 4 y 6 z 9]
  (match [x y z]
         [(:or 1 2 3) _ _] :a0
         [4 (:or 5 6 7) _] :a1
         :else []))
"))))
  (testing "a local isn't a pattern binding"
    (is (empty? (lint!! "
(let [a 1 b 1]
  (match [1 2]
         [a 3] :a1
         [1 2] :a2
         [2 b] :a5
         [_ 3] :a4
         :else :a3))
"))))
  (testing ":<<"
    (is (empty? (lint!! "
(match [1]
  [(1 :<< inc)] :one
  [(2 :<< inc)] :two
    :else :oops)"))))
  (testing ":when"
    (is (empty? (lint!! "
(let [y '(2 3 4 5)]
           (match [y]
             [([_ (a :when even?) _ _] :seq)] a
             [([_ (b :when [number? odd?]) _ _] :seq)] b
             :else []))

(let [y '(2 3 4 5)]
  (match [y]
    [([_ _ :when even? _ _] :seq)] :a0
    [([_ _ :when [number? odd?] _ _] :seq)] :a1
    :else []))")))))

(deftest same-named-binding-test
  (is (empty? (lint!! "
(defn match-or
  [x]
  (match
   [x]
   [(:or [\"x\" clause] [\"y\" clause])]
   clause
   :else :something))

(match-or [\"x\" :dude])
"))))
