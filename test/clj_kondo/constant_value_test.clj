(ns clj-kondo.constant-value-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest constant-value-test
  (testing "test linting constant value for clojure operator with message"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "1-arity use of clojure.core/= always evaluates to the same value"})
     (lint! "(= 1)")))

  (testing "test linting constant value for cljs operator with message"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "1-arity use of cljs.core/> always evaluates to the same value"})
     (lint! "(> 1)" "--lang" "cljs")))

  (testing "test linting constant value for all checked operators"
    (doseq [lang-name ["clj" "cljs"]
            op-name ["=" ">" "<" ">=" "<=" "and" "or" "not="]]
      (is (seq (lint! (str "(" op-name " 1)") "--lang" lang-name)))))

  (testing "test linting constant value in threading macros"
    (is (seq (lint! "(-> 10 (>))")))
    (is (seq (lint! "(->> 10 (>))")))
    (is (seq (lint! "(def x 11)(->> x (+ 1) (>))"))))

  (testing "test linting constant value for valid expressions"
    (doseq [expr ["(= 1 2)"
                  "(or true false)"
                  "(-> 10 (> 20))"
                  "(->> 10 (anfud 20))"
                  "(def x 11)(->> x (+ 1) (> 2))"]]
      (is (empty? (lint! expr))))))
