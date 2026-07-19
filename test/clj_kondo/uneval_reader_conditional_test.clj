(ns clj-kondo.uneval-reader-conditional-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps2]]
            [clojure.test :refer [deftest is testing]]))

(def ^:private discard-msg "#_ with unmatched reader conditional discards the next form")
(def ^:private delimiter-msg "#_ with unmatched reader conditional discards the closing delimiter")

(deftest uneval-reader-conditional-test
  (testing "unmatched conditional discards the next form"
    (assert-submaps2
     [{:row 1 :col 4 :level :warning :message discard-msg}]
     (lint! "[#_#?(:cljs 1) 2 3]" "--lang" "cljc"))
    (assert-submaps2
     [{:row 1 :col 4 :level :warning :message discard-msg}]
     (lint! "[#_#?(:clj 1) 2 3]" "--lang" "cljc")))
  (testing "unmatched conditional in final position"
    (assert-submaps2
     [{:row 1 :col 6 :level :error :message delimiter-msg}]
     (lint! "[1 #_#?(:cljs 2)]" "--lang" "cljc")))
  (testing "conditional covering all languages"
    (is (empty? (lint! "[#_#?(:clj 1 :cljs 2) 3]" "--lang" "cljc")))
    (is (empty? (lint! "[#_#?(:default 1) 2]" "--lang" "cljc")))))
