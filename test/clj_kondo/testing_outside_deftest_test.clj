(ns clj-kondo.testing-outside-deftest-test 
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is testing]]))

(deftest testing-outside-deftest-test
  (doseq [lang ["clj" "cljs"]]
    (let [lint! #(lint! % "--lang" lang)]
      (testing "testing inside deftest should not warn"
        (is (empty? (lint! "(ns foo (:require [clojure.test :as t])) (t/deftest foo (t/testing \"bar\" (t/is (= 1 1))))"))))

      (testing "testing outside deftest should warn"
        (assert-submaps2
         '({:file "<stdin>"
            :row 1
            :col 42
            :level :warning
            :message "testing called outside of deftest"})
         (lint! "(ns foo (:require [clojure.test :as t])) (t/testing \"bar\" (t/is (= 1 1)))")))

      (testing "nested testing inside deftest should not warn"
        (is (empty? (lint! "(ns foo (:require [clojure.test :as t])) (t/deftest foo (t/testing \"outer\" (t/testing \"inner\" (t/is (= 1 1)))))"))))

      (testing "testing inside let inside deftest should not warn"
        (is (empty? (lint! "(ns foo (:require [clojure.test :as t])) (t/deftest foo (let [x 1] (t/testing \"bar\" (t/is (= x 1)))))"))))

      (testing "testing at top level should warn"
        (assert-submaps2
         '({:file "<stdin>"
            :row 1
            :col 47
            :level :warning
            :message "testing called outside of deftest"})
         (lint! "(require '[clojure.test :refer [testing is]]) (testing \"bar\" (is (= 1 1)))")))

      (testing "testing inside regular function should warn"
        (assert-submaps2
         '({:file "<stdin>"
            :row 1
            :col 57
            :level :warning
            :message "testing called outside of deftest"})
         (lint! "(ns foo (:require [clojure.test :as t])) (defn my-fn [] (t/testing \"bar\" (t/is (= 1 1))))")))

      (testing "testing with refer should work"
        (is (empty? (lint! "(require '[clojure.test :refer [deftest testing is]]) (deftest foo (testing \"bar\" (is (= 1 1))))")))
        (assert-submaps2
         '({:file "<stdin>"
            :row 1
            :col 47
            :level :warning
            :message "testing called outside of deftest"})
         (lint! "(require '[clojure.test :refer [testing is]]) (testing \"bar\" (is (= 1 1)))"))))))
(comment 
(lint! "(deftest no-cache?
  (with-ns-binding 'nextjournal.clerk.analyzer-test
    (testing \"are variables set to no-cache?\"
      (is (not (:no-cache? (ana/analyze '(rand-int 10)))))
      (is (not (:no-cache? (ana/analyze '(def random-thing (rand-int 1000))))))
      (is (not (:no-cache? (ana/analyze '(defn random-thing [] (rand-int 1000))))))
      (is (:no-cache? (ana/analyze '^:nextjournal.clerk/no-cache (rand-int 10))))
      (is (:no-cache? (ana/analyze '^:nextjournal.clerk/no-cache (def random-thing (rand-int 1000)))))
      (is (:no-cache? (ana/analyze '^:nextjournal.clerk/no-cache (defn random-thing [] (rand-int 1000))))))


    (testing \"deprecated way to set no-cache?\"
      (is (:no-cache? (ana/analyze '(def ^:nextjournal.clerk/no-cache random-thing (rand-int 1000)))))
      (is (:no-cache? (ana/analyze '(defn ^:nextjournal.clerk/no-cache random-thing [] (rand-int 1000)))))
      (is (:no-cache? (ana/analyze '(defn ^{:nextjournal.clerk/no-cache true} random-thing [] (rand-int 1000)))))))

  (testing \"is evaluating namespace set to no-cache?\"
    (is (not (ana/no-cache? '(rand-int 10) (find-ns 'nextjournal.clerk.analyzer-test))))

    (is (nextjournal.clerk.analyzer/no-cache? '(rand-int 10) (find-ns 'nextjournal.clerk.analyzer)))))"))
