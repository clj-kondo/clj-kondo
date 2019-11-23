(ns clj-kondo.unused-import-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :refer [deftest testing is]]))

(deftest unused-import-test
  (testing "Detecting unused imports"
    (assert-submaps
     '({:file "<stdin>",
        :row 1,
        :col 21,
        :level :warning,
        :message "Unused import Foo"}
       {:file "<stdin>",
        :row 1,
        :col 25,
        :level :warning,
        :message "Unused import Bar"})
     (lint! "(import '[java.util Foo Bar])"))
    (assert-submaps
     '({:file "<stdin>",
        :row 1,
        :col 29,
        :level :warning,
        :message "Unused import Foo"}
       {:file "<stdin>",
        :row 1,
        :col 33,
        :level :warning,
        :message "Unused import Bar"})
     (lint! "(ns foo (:import [java.util Foo Bar]))"))
    (assert-submaps
     '({:file "<stdin>",
        :row 1,
        :col 10,
        :level :warning,
        :message "Unused import Foo"})
     (lint! "(import 'java.util.Foo)")))
  (testing "Preventing false positives"
    (is (empty? (lint! "(import '[java.util Foo Bar]) Foo Bar")))
    (is (empty? (lint! "(ns bar (:import [java.util Foo Bar])) Foo Bar")))
    (is (empty? (lint! "(import '[java.util Foo Bar]) Foo/CONSTANT (Bar/static_fn)")))
    (is (empty? (lint! "(import '[java.util Foo]) (defn foo [^Foo x] x)")))))
