(ns clj-kondo.blocking-inside-go-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps2]]
            [clojure.test :refer [deftest is testing]]))

(deftest blocking-inside-go-test
  (testing "<!! inside go emits warning"
    (assert-submaps2
      '({:file "<stdin>", :row 1, :col 45, :level :warning,
         :message "blocking operation inside go block"})
      (lint! "(require '[clojure.core.async :as a]) (a/go (a/<!! (a/chan)))")))

  (testing ">!! inside go emits warning"
    (assert-submaps2
      '({:file "<stdin>", :row 1, :col 45, :level :warning,
         :message "blocking operation inside go block"})
      (lint! "(require '[clojure.core.async :as a]) (a/go (a/>!! (a/chan) 1))")))

  (testing "alts!! inside go emits warning"
    (assert-submaps2
      '({:file "<stdin>", :row 1, :col 45, :level :warning,
         :message "blocking operation inside go block"})
      (lint! "(require '[clojure.core.async :as a]) (a/go (a/alts!! [(a/chan)]))")))

  (testing "<!! with refer inside go emits warning"
    (assert-submaps2
      '({:file "<stdin>", :row 1, :col 58, :level :warning,
         :message "blocking operation inside go block"})
      (lint! "(require '[clojure.core.async :refer [go <!! chan]]) (go (<!! (chan)))")))

  (testing ">!! with refer inside go emits warning"
    (assert-submaps2
      '({:file "<stdin>", :row 1, :col 58, :level :warning,
         :message "blocking operation inside go block"})
      (lint! "(require '[clojure.core.async :refer [go >!! chan]]) (go (>!! (chan) 1))")))

  (testing "nested expression inside go emits warning"
    (assert-submaps2
      '({:file "<stdin>", :row 1, :col 54, :level :warning,
         :message "blocking operation inside go block"})
      (lint! "(require '[clojure.core.async :as a]) (a/go (println (a/<!! (a/chan))))")))

  (testing "deeper nesting inside go emits warning"
    (assert-submaps2
      '({:file "<stdin>", :row 1, :col 67, :level :warning,
         :message "blocking operation inside go block"})
      (lint! "(require '[clojure.core.async :as a]) (a/go (let [x 1] (when true (a/<!! (a/chan)))))")))

  (testing "multiple blocking calls inside go emits multiple warnings"
    (assert-submaps2
      '({:file "<stdin>", :row 1, :col 45, :level :warning,
         :message "blocking operation inside go block"}
        {:file "<stdin>", :row 1, :col 56, :level :warning,
         :message "blocking operation inside go block"})
      (lint! "(require '[clojure.core.async :as a]) (a/go (a/<!! ch) (a/>!! ch 1))"
             {:linters {:unused-binding {:level :off}
                        :unresolved-symbol {:level :off}}})))

  (testing "<!! inside go-loop emits warning"
    (assert-submaps2
      '({:file "<stdin>", :row 1, :col 53, :level :warning,
         :message "blocking operation inside go block"})
      (lint! "(require '[clojure.core.async :as a]) (a/go-loop [] (a/<!! (a/chan)) (recur))")))

  (testing ">!! inside go-loop emits warning"
    (assert-submaps2
      '({:file "<stdin>", :row 1, :col 53, :level :warning,
         :message "blocking operation inside go block"})
      (lint! "(require '[clojure.core.async :as a]) (a/go-loop [] (a/>!! (a/chan) 1) (recur))")))

  (testing "alts!! inside go-loop emits warning"
    (assert-submaps2
      '({:file "<stdin>", :row 1, :col 53, :level :warning,
         :message "blocking operation inside go block"})
      (lint! "(require '[clojure.core.async :as a]) (a/go-loop [] (a/alts!! [(a/chan)]) (recur))")))

  (testing "nested blocking inside go-loop emits warning"
    (assert-submaps2
      '({:file "<stdin>", :row 1, :col 62, :level :warning,
         :message "blocking operation inside go block"})
      (lint! "(require '[clojure.core.async :as a]) (a/go-loop [] (println (a/<!! (a/chan))) (recur))")))

  (testing "deeper nesting inside go-loop emits warning"
    (assert-submaps2
      '({:file "<stdin>", :row 1, :col 75, :level :warning,
         :message "blocking operation inside go block"})
      (lint! "(require '[clojure.core.async :as a]) (a/go-loop [] (let [x 1] (when true (a/<!! (a/chan)))) (recur))"))))

(deftest no-blocking-inside-go-test
  (testing "<! (parking, not blocking) inside go is OK"
    (is (empty?
          (lint! "(require '[clojure.core.async :as a]) (a/go (a/<! (a/chan)))"))))

  (testing ">! (parking, not blocking) inside go is OK"
    (is (empty?
          (lint! "(require '[clojure.core.async :as a]) (a/go (a/>! (a/chan) 1))"))))

  (testing "alts! (parking) inside go is OK"
    (is (empty?
          (lint! "(require '[clojure.core.async :as a]) (a/go (a/alts! [(a/chan)]))"))))

  (testing "<!! outside go is OK"
    (is (empty?
          (lint! "(require '[clojure.core.async :as a]) (a/<!! (a/chan))"))))

  (testing ">!! outside go is OK"
    (is (empty?
          (lint! "(require '[clojure.core.async :as a]) (a/>!! (a/chan) 1)"))))

  (testing "alts!! outside go is OK"
    (is (empty?
          (lint! "(require '[clojure.core.async :as a]) (a/alts!! [(a/chan)])"))))

  (testing "<!! inside thread (not go) is OK"
    (is (empty?
          (lint! "(require '[clojure.core.async :as a]) (a/thread (a/<!! (a/chan)))"))))

  (testing ">!! inside thread (not go) is OK"
    (is (empty?
          (lint! "(require '[clojure.core.async :as a]) (a/thread (a/>!! (a/chan) 1))"))))

  (testing "alts!! inside thread (not go) is OK"
    (is (empty?
          (lint! "(require '[clojure.core.async :as a]) (a/thread (a/alts!! [(a/chan)]))"))))

  (testing "<!! inside future is OK"
    (is (empty?
          (lint! "(require '[clojure.core.async :as a]) (future (a/<!! (a/chan)))"))))

  (testing "go without blocking is OK"
    (is (empty?
          (lint! "(require '[clojure.core.async :as a]) (a/go (println \"safe\"))"))))

  (testing "shadowed <!! should NOT trigger"
    (is (empty?
          (lint! "(require '[clojure.core.async :as a]) (let [<!! (fn [_] :fake)] (a/go (<!! (a/chan))))"))))

  (testing "linter can be disabled via config"
    (is (empty?
          (lint! "(require '[clojure.core.async :as a]) (a/go (a/<!! (a/chan)))"
                 {:linters {:blocking-inside-go {:level :off}}})))))