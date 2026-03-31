(ns clj-kondo.not-nil-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps2]]
   [clojure.test :refer [deftest is testing]]))

(def ^:private config {:linters {:not-nil? {:level :warning}}})

(deftest not-nil?-test
  (testing "(not (nil? x)) -> (some? x)"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 6, :level :warning,
        :message "Use (some? x) instead of (not (nil? x))"})
     (lint! "(not (nil? 1))" config)))

  (testing "(when-not (nil? x) ...) -> (when (some? x) ...)"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 11, :level :warning,
        :message "Use (when (some? x) ...) instead of (when-not (nil? x) ...)"})
     (lint! "(when-not (nil? 1) :foo)" config)))

  (testing "(if-not (nil? x) ...) -> (if (some? x) ...)"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 9, :level :warning,
        :message "Use (if (some? x) ...) instead of (if-not (nil? x) ...)"})
     (lint! "(if-not (nil? 1) :foo :bar)" config)))

  (testing "works in ClojureScript"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 6, :level :warning,
        :message "Use (some? x) instead of (not (nil? x))"})
     (lint! "(not (nil? 1))" config "--lang" "cljs")))

  (testing "no false positives"
    (is (empty? (lint! "(nil? 1)" config)))
    (is (empty? (lint! "(when (nil? 1) :foo)" config)))
    (is (empty? (lint! "(if (nil? 1) :foo :bar)" config)))))

(deftest not-nil?-ignore-test
  (testing "no warning when linter is disabled"
    (is (empty? (lint! "(not (nil? 1))"
                       {:linters {:not-nil? {:level :off}}}))))

  (testing "fires at default level (info)"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 6, :level :info,
        :message "Use (some? x) instead of (not (nil? x))"})
     (lint! "(not (nil? 1))")))

  (testing "linter-specific ignore suppresses not-nil?"
    (is (empty? (lint! "#_{:clj-kondo/ignore [:not-nil?]} (not (nil? 1))" config)))
    (is (empty? (lint! "#_{:clj-kondo/ignore [:not-nil?]} (when-not (nil? 1) :foo)" config)))
    (is (empty? (lint! "#_{:clj-kondo/ignore [:not-nil?]} (if-not (nil? 1) :foo :bar)" config))))

  (testing "linter-specific ignore with inline metadata"
    (is (empty? (lint! "^{:clj-kondo/ignore [:not-nil?]} (not (nil? 1))" config))))

  (testing "bare ^:clj-kondo/ignore suppresses all linters"
    (is (empty? (lint! "#_:clj-kondo/ignore (not (nil? 1))" config))))

  (testing "linter-specific ignore does not suppress unrelated linter"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 45, :level :warning,
        :message "Use (some? x) instead of (not (nil? x))"})
     (lint! "#_{:clj-kondo/ignore [:invalid-arity]} (not (nil? 1))" config))))
