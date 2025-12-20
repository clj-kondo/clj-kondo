(ns clj-kondo.refer-clojure-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is testing]]))

(deftest refer-clojure-exclude-test
  (testing "clj"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 35
        :level :warning
        :message "The var foo does not exist in clojure.core"})
     (lint! "(ns foo (:refer-clojure :exclude [foo]))")))
  (testing "clj valid"
    (is (empty? (lint! "(ns foo (:refer-clojure :exclude [map]))"))))
  (testing "cljs"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 35
        :level :warning
        :message "The var future does not exist in cljs.core"})
     (lint! "(ns foo (:refer-clojure :exclude [future]))"
            "--lang" "cljs")))
  (testing "cljs valid"
    (is (empty? (lint! "(ns foo (:refer-clojure :exclude [js-obj]))"
                       "--lang" "cljs"))))
  (testing "cljc valid in clj and cljs"
    (is (empty? (lint! "(ns foo (:refer-clojure :exclude [map]))"
                       "--lang" "cljc"))))
  (testing "cljc valid in cljs"
    (is (empty?
         (lint! "(ns foo (:refer-clojure :exclude [js-obj]))"
                "--lang" "cljc"))))
  (testing "cljc valid in clj"
    (is (empty? (lint! "(ns foo (:refer-clojure :exclude [future map]))"
                       "--lang" "cljc"))))
  (testing "cljc invalid in clj and cljs"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 35
        :level :warning
        :message "The var bad-cljs-var does not exist in clojure.core"}
       {:file "<stdin>"
        :row 1
        :col 35
        :level :warning
        :message "The var bad-cljs-var does not exist in cljs.core"})
     (lint! "(ns foo (:refer-clojure :exclude [bad-cljs-var js-obj]))"
            "--lang" "cljc"))))

(deftest refer-clojure-multiple-exclude-test
  (testing "multiple vars with some valid and some invalid"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 35
        :level :warning
        :message "The var foo does not exist in clojure.core"}
       {:file "<stdin>"
        :row 1
        :col 43
        :level :warning
        :message "The var bar does not exist in clojure.core"})
     (lint! "(ns foo (:refer-clojure :exclude [foo map bar filter]))")))
  (testing "multiple vars across multiple lines"
    (assert-submaps2
     '({:file "<stdin>"
        :row 2
        :col 29
        :level :warning
        :message "The var invalid-var does not exist in clojure.core"}
       {:file "<stdin>"
        :row 3
        :col 29
        :level :warning
        :message "The var another-bad does not exist in clojure.core"})
     (lint! "(ns foo
  (:refer-clojure :exclude [invalid-var map
                            another-bad filter]))")))
  (testing "cljs multiple vars with mix of valid and invalid"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 35
        :level :warning
        :message "The var bad-cljs-var does not exist in cljs.core"}
       {:file "<stdin>"
        :row 1
        :col 55
        :level :warning
        :message "The var future does not exist in cljs.core"})
     (lint! "(ns foo (:refer-clojure :exclude [bad-cljs-var js-obj future map]))"
            "--lang" "cljs"))))

(deftest refer-clojure-disabled-test
  (testing "linter disabled via config"
    (is (empty? (lint! "(ns foo (:refer-clojure :exclude [nonexistent]))"
                       {:linters {:refer-clojure-exclude-non-existing-var
                                  {:level :off}}}))))
  (testing "linter disabled for specific invalid var"
    (is (empty? (lint! "(ns foo (:refer-clojure :exclude [fake-var]))"
                       {:linters {:refer-clojure-exclude-non-existing-var
                                  {:level :off}}}))))
  (testing "linter disabled in specific namespace with config-in-ns"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 35
        :level :warning
        :message "The var nonexistent does not exist in clojure.core"})
     (lint! "(ns foo (:refer-clojure :exclude [nonexistent]))

(ns bar
  {:clj-kondo/config {:linters {:refer-clojure-exclude-non-existing-var {:level :off}}}}
  (:refer-clojure :exclude [nonexistent]))"
            {:linters {:refer-clojure-exclude-non-existing-var
                       {:level :warning}}}))))
