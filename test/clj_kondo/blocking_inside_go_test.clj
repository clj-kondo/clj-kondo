(ns clj-kondo.blocking-inside-go-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps2]]
            [clojure.test :refer [deftest is testing]]))

(deftest blocking-inside-go-test
  (testing "<!! inside go emits warning"
    (assert-submaps2
     '({:file "<stdin>",
        :row 1,
        :col 45,
        :level :warning,
        :message "blocking operation inside go block"})
     (lint! "(require '[clojure.core.async :as a]) (a/go (a/<!! (a/chan)))")))

  (testing ">!! inside go emits warning"
    (assert-submaps2
     '({:file "<stdin>",
        :row 1,
        :col 45,
        :level :warning,
        :message "blocking operation inside go block"})
     (lint! "(require '[clojure.core.async :as a]) (a/go (a/>!! (a/chan) 1))")))

  (testing "<!! with refer inside go emits warning"
    (assert-submaps2
     '({:file "<stdin>",
        :row 1,
        :col 53,
        :level :warning,
        :message "blocking operation inside go block"})
     (lint! "(require '[clojure.core.async :refer [go <!!]]) (go (<!! (chan)))")))

  (testing ">!! with refer inside go emits warning"
    (assert-submaps2
     '({:file "<stdin>",
        :row 1,
        :col 53,
        :level :warning,
        :message "blocking operation inside go block"})
     (lint! "(require '[clojure.core.async :refer [go >!!]]) (go (>!! (chan) 1))")))

  (testing "multiple blocking calls inside go emits multiple warnings"
    (assert-submaps2
     '({:file "<stdin>",
        :row 1,
        :col 45,
        :level :warning,
        :message "blocking operation inside go block"}
       {:file "<stdin>",
        :row 1,
        :col 56,
        :level :warning,
        :message "blocking operation inside go block"})
     (lint! "(require '[clojure.core.async :as a]) (a/go (a/<!! ch) (a/>!! ch 1))"
            {:linters {:unused-binding {:level :off}
                       :unresolved-symbol {:level :off}}}))))

(deftest no-blocking-inside-go-test
  (testing "<! (parking, not blocking) inside go is OK"
    (is (empty?
         (lint! "(require '[clojure.core.async :as a]) (a/go (a/<! (a/chan)))"))))

  (testing ">! (parking, not blocking) inside go is OK"
    (is (empty?
         (lint! "(require '[clojure.core.async :as a]) (a/go (a/>! (a/chan) 1))"))))

  (testing "<!! outside go is OK"
    (is (empty?
         (lint! "(require '[clojure.core.async :as a]) (a/<!! (a/chan))"))))

  (testing ">!! outside go is OK"
    (is (empty?
         (lint! "(require '[clojure.core.async :as a]) (a/>!! (a/chan) 1)"))))

  (testing "<!! inside thread (not go) is OK"
    (is (empty?
         (lint! "(require '[clojure.core.async :as a]) (a/thread (a/<!! (a/chan)))"))))

  (testing ">!! inside thread (not go) is OK"
    (is (empty?
         (lint! "(require '[clojure.core.async :as a]) (a/thread (a/>!! (a/chan) 1))"))))

  (testing "linter can be disabled via config"
    (is (empty?
         (lint! "(require '[clojure.core.async :as a]) (a/go (a/<!! (a/chan)))"
                {:linters {:blocking-inside-go {:level :off}}})))))
