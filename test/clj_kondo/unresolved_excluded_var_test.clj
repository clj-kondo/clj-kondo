(ns clj-kondo.unresolved-excluded-var-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is testing]]))

(def config {:linters {:unused-excluded-var {:level :off}}})

(deftest refer-clojure-exclude-test
  (testing "exclude with ignored element only warns for non-ignored (#2691)"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 61
        :level :info
        :message "Unresolved excluded var: foo"})
     (lint! "(ns foo (:refer-clojure :exclude [#_:clj-kondo/ignore comp2 foo]))")))

  (testing "linter-specific ignore does not suppress unrelated linter"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 74
        :level :info
        :message "Unresolved excluded var: foo"})
     (lint! "(ns foo (:refer-clojure :exclude [#_{:clj-kondo/ignore [:invalid-arity]} foo]))")))

  (testing "clj"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 35
        :level :info
        :message "Unresolved excluded var: foo"})
     (lint! "(ns foo (:refer-clojure :exclude [foo]))")))
  (testing "clj valid"
    (is (empty? (lint! "(ns foo (:refer-clojure :exclude [map]))"
                       config))))
  (testing "cljs"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 35
        :level :info
        :message "Unresolved excluded var: future"})
     (lint! "(ns foo (:refer-clojure :exclude [future]))"
            "--lang" "cljs"
            "--config" (pr-str config))))
  (testing "cljs valid"
    (is (empty? (lint! "(ns foo (:refer-clojure :exclude [munge-str]))"
                       "--lang" "cljs"
                       "--config" (pr-str config)))))
  (testing "cljs valid"
    (is (empty? (lint! "(ns foo (:refer-clojure :exclude [js-obj]))"
                       "--lang" "cljs"
                       "--config" (pr-str config)))))
  (testing "cljc valid in clj and cljs"
    (is (empty? (lint! "(ns foo (:refer-clojure :exclude [map]))"
                       "--lang" "cljc"
                       "--config" (pr-str config)))))
  (testing "cljc valid in cljs"
    (is (empty?
         (lint! "(ns foo (:refer-clojure :exclude [js-obj]))"
                "--lang" "cljc"
                "--config" (pr-str config)))))
  (testing "cljc valid in clj"
    (is (empty? (lint! "(ns foo (:refer-clojure :exclude [future map]))"
                       "--lang" "cljc"
                       "--config" (pr-str config)))))
  (testing "cljc invalid in clj and cljs"
    (assert-submaps2
     [{:file "<stdin>"
       :row 1
       :col 35
       :level :info
       :message "Unresolved excluded var: bad-cljs-var"}]
     (lint! "(ns foo (:refer-clojure :exclude [bad-cljs-var js-obj]))"
            "--lang" "cljc"
            "--config" (pr-str config)))))

(deftest refer-clojure-multiple-exclude-test
  (testing "multiple vars with some valid and some invalid"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 35
        :level :info
        :message "Unresolved excluded var: foo"}
       {:file "<stdin>"
        :row 1
        :col 43
        :level :info
        :message "Unresolved excluded var: bar"})
     (lint! "(ns foo (:refer-clojure :exclude [foo map bar filter]))"
            config)))
  (testing "multiple vars across multiple lines"
    (assert-submaps2
     '({:file "<stdin>"
        :row 2
        :col 29
        :level :info
        :message "Unresolved excluded var: invalid-var"}
       {:file "<stdin>"
        :row 3
        :col 29
        :level :info
        :message "Unresolved excluded var: another-bad"})
     (lint! "(ns foo
  (:refer-clojure :exclude [invalid-var map
                            another-bad filter]))"
            config)))
  (testing "cljs multiple vars with mix of valid and invalid"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 35
        :level :info
        :message "Unresolved excluded var: bad-cljs-var"}
       {:file "<stdin>"
        :row 1
        :col 55
        :level :info
        :message "Unresolved excluded var: future"})
     (lint! "(ns foo (:refer-clojure :exclude [bad-cljs-var js-obj future map]))"
            "--lang" "cljs"
            "--config" (pr-str config)))))

(deftest refer-clojure-disabled-test
  (testing "linter disabled via config"
    (is (empty? (lint! "(ns foo (:refer-clojure :exclude [nonexistent]))"
                       {:linters {:unresolved-excluded-var
                                  {:level :off}}}))))
  (testing "linter disabled for specific invalid var"
    (is (empty? (lint! "(ns foo (:refer-clojure :exclude [fake-var]))"
                       {:linters {:unresolved-excluded-var
                                  {:level :off}}}))))
  (testing "linter disabled in specific namespace with config-in-ns"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 35
        :level :info
        :message "Unresolved excluded var: nonexistent"})
     (lint! "(ns foo (:refer-clojure :exclude [nonexistent]))

(ns bar
  {:clj-kondo/config {:linters {:unresolved-excluded-var {:level :off}}}}
  (:refer-clojure :exclude [nonexistent]))"
            {:linters {:unresolved-excluded-var
                       {:level :info}}}))))

(deftest refer-clojure-special-symbol-test
  (testing "special symbols warn in clj"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 35
        :level :info
        :message "Unresolved excluded var: def"})
     (lint! "(ns foo (:refer-clojure :exclude [def]))")))
  (testing "special symbols warn in cljs"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 35
        :level :info
        :message "Unresolved excluded var: def"})
     (lint! "(ns foo (:refer-clojure :exclude [def]))"
            "--lang" "cljs"
            "--config" (pr-str config))))
  (testing "special symbols warn in cljc"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 35
        :level :info
        :message "Unresolved excluded var: def"})
     (lint! "(ns foo (:refer-clojure :exclude [def])) def"
            "--lang" "cljc"
            "--config" (pr-str config))))
  (testing "mix of special symbols and regular core symbols"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 35
        :level :info
        :message "Unresolved excluded var: def"}
       {:file "<stdin>"
        :row 1
        :col 43
        :level :info
        :message "Unresolved excluded var: if"})
     (lint! "(ns foo (:refer-clojure :exclude [def map if filter]))"
            {:linters {:unused-excluded-var {:level :off}}}))))

(deftest refer-clojure-special-forms-test
  (testing ".. is valid in clj"
    (is (empty? (lint! "(ns foo (:refer-clojure :exclude [..]))"
                       config))))
  (testing "special-forms are valid in cljs"
    (is (empty? (lint! "(ns foo (:refer-clojure :exclude [..]))"
                       "--lang" "cljs"
                       "--config" (pr-str config)))))
  (testing "special-forms are valid in cljc"
    (is (empty? (lint! "(ns foo (:refer-clojure :exclude [..])) .."
                       "--lang" "cljc"
                       "--config" (pr-str config)))))
  (testing "mix of special-forms and regular core symbols"
    (is (empty? (lint! "(ns foo (:refer-clojure :exclude [.. map loop filter]))"
                       {:linters {:unused-excluded-var {:level :off}}})))))
