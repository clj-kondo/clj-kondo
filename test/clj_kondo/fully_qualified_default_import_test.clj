(ns clj-kondo.fully-qualified-default-import-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps2]]
            [clojure.test :refer [deftest is testing]]))

(def warning-config {:linters {:fully-qualified-default-import {:level :warning}}})

(defn- msg [fq-name simple-name]
  (format "Fully qualified class %s can be simplified to %s (available by default)"
          fq-name simple-name))

(deftest fully-qualified-java-lang-test
  (testing "warns when using fully qualified java.lang class"
    (assert-submaps2 [{:row 1
                       :col 13
                       :level :warning
                       :message (msg "java.lang.Thread" "Thread")}]
                     (lint! "(try (catch java.lang.Thread e))" warning-config)))
  (testing "warns when calling static method on fully qualified class"
    (assert-submaps2 [{:row 1
                       :col 1
                       :level :warning
                       :message (msg "java.lang.Thread" "Thread")}]
                     (lint! "(java.lang.Thread/sleep 1000)" warning-config)))
  (testing "warns when instantiating with fully qualified class"
    (assert-submaps2 [{:row 1
                       :col 1
                       :level :warning
                       :message (msg "java.lang.String" "String")}]
                     (lint! "(new java.lang.String \"hello\")" warning-config)))
  (testing "does not warn when using simple class name"
    (is (empty? (lint! "(try (catch Thread e))" warning-config))))
  (testing "does not warn when using simple class name for static method"
    (is (empty? (lint! "(Thread/sleep 1000)" warning-config))))
  (testing "does not warn when using simple class name for instantiation"
    (is (empty? (lint! "(new String \"hello\")" warning-config)))))

(deftest fully-qualified-java-math-test
  (testing "warns for java.math.BigDecimal"
    (assert-submaps2 [{:row 1
                       :col 1
                       :level :warning
                       :message (msg "java.math.BigDecimal" "BigDecimal")}]
                     (lint! "(java.math.BigDecimal/valueOf 1)" warning-config)))
  (testing "warns for java.math.BigInteger"
    (assert-submaps2 [{:row 1
                       :col 1
                       :level :warning
                       :message (msg "java.math.BigInteger" "BigInteger")}]
                     (lint! "(java.math.BigInteger/valueOf 42)" warning-config)))
  (testing "does not warn when using simple name"
    (is (empty? (lint! "(BigDecimal/valueOf 1)" warning-config)))))

(deftest fully-qualified-clojure-lang-test
  (testing "warns for clojure.lang.Compiler"
    (assert-submaps2 [{:row 1
                       :col 13
                       :level :warning
                       :message (msg "clojure.lang.Compiler" "Compiler")}]
                     (lint! "(try (catch clojure.lang.Compiler e))" warning-config)))
  (testing "does not warn when using simple name"
    (is (empty? (lint! "(try (catch Compiler e))" warning-config)))))

(deftest no-warning-for-non-default-imports-test
  (testing "does not warn for java.util classes"
    (is (empty? (lint! "(java.util.Date.)" warning-config))))
  (testing "does not warn for java.io classes"
    (is (empty? (lint! "(java.io.File. \"/tmp\")" warning-config))))
  (testing "does not warn for custom classes"
    (is (empty? (lint! "(com.example.MyClass.)" warning-config)))))

(deftest multiple-usages-test
  (testing "warns for each usage of fully qualified default import"
    (assert-submaps2 [{:row 1
                       :col 1
                       :level :warning
                       :message (msg "java.lang.Thread" "Thread")}
                      {:row 2
                       :col 1
                       :level :warning
                       :message (msg "java.lang.String" "String")}]
                     (lint! "(java.lang.Thread/sleep 1000)
(java.lang.String/valueOf 42)" warning-config))))

(deftest linter-disabled-test
  (testing "does not warn when linter is disabled"
    (is (empty? (lint! "(java.lang.Thread/sleep 1000)"
                       {:linters {:fully-qualified-default-import {:level :off}}})))))
