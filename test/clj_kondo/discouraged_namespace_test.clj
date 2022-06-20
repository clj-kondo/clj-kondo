(ns clj-kondo.discouraged-namespace-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest discouraged-namespace-test
  (assert-submaps
   '({:file "<stdin>" :row 1 :col 12 :level :error :message "Discouraged namespace: closed.source"})
   (lint! "(require '[closed.source :as s]) (s/baz)"
          '{:linters {:discouraged-namespace {closed.source {}
                                              :level :error}}}))

  (assert-submaps
   '({:file "<stdin>", :row 1, :col 12, :level :warning, :message "Don't use `closed.source`"})
   (lint! "(require '[closed.source :as s]) (s/baz)"
          '{:linters {:discouraged-namespace {closed.source {:message "Don't use `closed.source`"}}}}))

  (testing "config-in-ns"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 24, :level :warning, :message "Don't use `closed.source`"})
     (lint! "(ns foo.bar (:require [closed.source :as s])) (s/baz)"
            '{:config-in-ns {foo.bar {:linters
                                      {:discouraged-namespace
                                       {closed.source {:message "Don't use `closed.source`"}}}}}})))

  (testing "ns-groups"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 12, :level :warning, :message "Discouraged namespace: closed.source"})
     (lint! "(require '[closed.source :as s]) (s/foo)"
            '{:ns-groups [{:pattern "closed\\..*"
                           :name closed}]
              :linters {:discouraged-namespace {closed {}}}}))))
