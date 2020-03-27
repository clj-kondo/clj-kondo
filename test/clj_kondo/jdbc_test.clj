(ns clj-kondo.jdbc-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps lint!]]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is]]
   [missing.test.assertions]))

(deftest next-jdbc-test
  (is (empty? (lint! (io/file "corpus" "jdbc" "next_test.clj")
                     {:linters {:unresolved-symbol {:level :error}}}))))

(deftest clojure-java-jdbc-test
  (is (empty? (lint! (io/file "corpus" "jdbc" "cjj_test.clj")
                     {:linters {:unresolved-symbol {:level :error}}}))))

(deftest detected-issues-test
  (assert-submaps '({:file "corpus/jdbc/next.clj", :row 9, :col 24, :level :error,
                     :message "with-transaction binding form requires exactly 2 or 3 forms"}
                    {:file "corpus/jdbc/next.clj", :row 12, :col 24, :level :error,
                     :message "with-transaction binding form requires exactly 2 or 3 forms"}
                    {:file "corpus/jdbc/next.clj", :row 16, :col 3, :level :error,
                     :message "with-transaction requires a vector for its binding"}
                    {:file "corpus/jdbc/next.clj", :row 18, :col 25, :level :error,
                     :message "with-transaction binding form requires a symbol"})
                  (lint! (io/file "corpus" "jdbc" "next.clj"))))
