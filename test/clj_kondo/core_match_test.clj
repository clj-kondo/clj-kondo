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
   '({:file "<stdin>", :row 3, :col 8, :level :warning, :message "unused binding a"})
   (lint! "(require '[clojure.core.match :refer [match]])
(match [1 2 3]
  [1 2 a] :foo)
" {:linters {:unused-binding {:level :warning}
             :unresolved-symbol {:level :error}}}))
  (assert-submaps
   '({:file "<stdin>", :row 5, :col 15, :level :warning, :message "unused binding b"}
     {:file "<stdin>", :row 7, :col 20, :level :warning, :message "unused binding x"})
   (lint!! "
(let [x {:a 1 :b 1}]
  (match [x]
         [{:a b :b 2}] :a0
         [{:a 1 :b 1}] :a1
         [{:c 3 :d x :e 4}] :a2
         :else nil))"))
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

  (testing "map pattern"
    (is (empty? (lint! "(require '[clojure.core.match :refer [match]])
(let [x {:a 1 :b 1}]
  (match [x]
         [{:a _ :b 2}] :a0
         [{:a 1 :b 1}] :a1
         [{:c 3 :d _ :e 4}] :a2
         :else nil))
" {:linters {:unused-binding {:level :warning}
             :unresolved-symbol {:level :error}}})))))
