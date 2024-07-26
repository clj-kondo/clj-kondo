(ns clj-kondo.discouraged-namespace-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps2]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest discouraged-namespace-test
  (assert-submaps2
   '({:file "<stdin>" :row 1 :col 12 :level :error :message "Discouraged namespace: closed.source"})
   (lint! "(require '[closed.source :as s]) (s/baz)"
          '{:linters {:discouraged-namespace {closed.source {}
                                              :level :error}}}))
  (assert-submaps2
   '({:file "<stdin>" :row 1 :col 12 :level :error :message "Discouraged namespace: closed.source"})
   (lint! "(require '[closed.source :as s]) (s/baz)"
          '{:linters {:discouraged-namespace {closed.source {:level :error}}}}))

  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 12, :level :warning, :message "Don't use `closed.source`"})
   (lint! "(require '[closed.source :as s]) (s/baz)"
          '{:linters {:discouraged-namespace {closed.source {:message "Don't use `closed.source`"}}}}))

  (testing "config-in-ns"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 24, :level :warning, :message "Don't use `closed.source`"})
     (lint! "(ns foo.bar (:require [closed.source :as s])) (s/baz)"
            '{:config-in-ns {foo.bar {:linters
                                      {:discouraged-namespace
                                       {closed.source {:message "Don't use `closed.source`"}}}}}})))
  (testing "ns-groups"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 12, :level :warning, :message "Discouraged namespace: closed.source"})
     (lint! "(require '[closed.source :as s]) (s/foo)"
            '{:ns-groups [{:pattern "closed\\..*"
                           :name closed}]
              :linters {:discouraged-namespace {closed {}}}}))))

(deftest ignore-test
  (testing "ignore in ns form"
    (is (empty?
         (lint! "(ns foo (:require #_:clj-kondo/ignore [closed.source :as s])) (s/foo)"
                '{:ns-groups [{:pattern "closed\\..*"
                               :name closed}]
                  :linters {:discouraged-namespace {closed {}}}})))))

(deftest ns-groups-test
  (testing "ns-groups"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 12, :level :warning, :message "Discouraged namespace: closed.source"})
     (lint! "(require '[closed.source :as s]) (s/foo)"
            '{:ns-groups [{:pattern "closed\\..*"
                           :name closed}]
              :linters {:discouraged-namespace {closed {}}}})))
  (testing "ns-groups"
    (assert-submaps2
     [{:row 1,
       :col 12,
       :level :warning,
       :message "Use next.jdbc instead of clojure.java.jdbc"}]
     (lint! "(require '[clojure.java.jdbc])"
            '{:ns-groups
              [{:pattern "^metabase\\.db\\..*"
                :name    db-namespaces}
               {:pattern "^clojure\\.java\\.jdbc.*"
                :name    jdbc-legacy}
               {:pattern "^clojure\\."
                :name    clojure-core-namespaces}]
              :linters {:discouraged-namespace
                        {clojure.java.jdbc {:message "Use next.jdbc instead of clojure.java.jdbc"}}}}))))
