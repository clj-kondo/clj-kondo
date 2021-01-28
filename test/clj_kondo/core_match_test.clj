(ns clj-kondo.core-match-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps lint!]]
   [clojure.test :as t :refer [deftest is testing]]
   [missing.test.assertions]))

(deftest core-match-test
  (is (empty? (lint! "(require '[clojure.core.match :refer [match]])

(match [1 2 3]
  [1 2 _] :foo)
"
                     {:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(require '[clojure.core.match :refer [match]])

(match [1 2 3]
  [1 2 a] a)
"
                     {:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(require '[clojure.core.match :refer [match]])

(match [1 2 [3 4]]
  [1 2 [a b]] [a b])
"
                     {:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(require '[clojure.core.match :refer [match]])

(match [1 2 [3 4]]
  [1 2 [a b]] [a b])
"
                     {:linters {:unresolved-symbol {:level :error}}})))
  (testing "& rest pattern"
    (is (empty? (lint! "(require '[clojure.core.match :refer [match]])
(let [x [1 2 3]]
  (match x
         [1 & rs] rs))
"
                       {:linters {:unused-binding {:level :warning}
                                  :unresolved-symbol {:level :error}}}))))
  (testing "& rest pattern"
    (is (empty? (lint! "(require '[clojure.core.match :refer [match]])
(let [x [1 2 nil nil nil]]
  (match [x]
    [([1] :seq)] :a0
    [([1 2] :seq)] :a1
    [([1 2 nil nil nil] :seq)] :a2
    :else nil))
;=> :a2
"
                       {:linters {:unused-binding {:level :warning}
                                  :unresolved-symbol {:level :error}}}))))
  (testing "nested seqs"
    (is (empty? (lint! "(require '[clojure.core.match :refer [match]])
(let [x '((1 2))]
  (match x
         ([([1 & r] :seq)] :seq) [:a1 r]))
"
                       {:linters {:unused-binding {:level :warning}
                                  :unresolved-symbol {:level :error}}}))))
  (testing "symbolic clause"
    (is (empty? (lint! "(require '[clojure.core.match :refer [match]])
(let [x '((1 2))]
  (match x
         y y))

" {:linters {:unused-binding {:level :warning}
             :unresolved-symbol {:level :error}}}))))

  (assert-submaps
   '({:file "<stdin>", :row 3, :col 8, :level :warning, :message "unused binding a"})
   (lint! "(require '[clojure.core.match :refer [match]])
(match [1 2 3]
  [1 2 a] :foo)
" {:linters {:unused-binding {:level :warning}
             :unresolved-symbol {:level :error}}})))

;; TODO:

#_(let [x '(1 2 4)
      y nil
      z nil]
  (match [x y z]
         [([1 2 b] :seq) _ _] [:a0 b]
         [([a 2 4] :seq) _ _] [:a1 a]
         :else []))
