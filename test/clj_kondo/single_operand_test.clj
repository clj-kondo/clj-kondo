(ns clj-kondo.single-operand-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest single-operand-comparison-test
  (testing "test full linting error for single operand comparison in clojure"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Single operand use of clojure.core/= is always true"})
     (lint! "(= 1)")))

  (testing "test full linting error for single operand comparison in cljs"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Single operand use of cljs.core/not= is always false"})
     (lint! "(not= 1)" "--lang" "cljs")))

  (testing "test linting comparison operators with single operand"
    (doseq [lang ["clj" "cljs"]
            op ["=" ">" "<" ">=" "<=" "=="]
            :let [errors (lint! (str "(" op " 1)") "--lang" lang)]]
      (is (= 1 (count errors)))
      (is (= (format "Single operand use of %s.core/%s is always true"
                     (get {"clj" "clojure"} lang "cljs")
                     op)
             (-> errors
                 (first)
                 :message)))))

  (testing "test linting single operand comparison in threading macros"
    (is (seq (lint! "(-> 10 (>))")))
    (is (seq (lint! "(->> 10 (>))")))
    (is (seq (lint! "(def x 11)(->> x (+ 1) (>))"))))

  (testing "test linting single operand comparison for valid expressions"
    (doseq [expr ["(= 1 2)"
                  "(= (== 1 2) false)"
                  "(-> 10 (> 20))"
                  "(->> 10 (< 20))"
                  "(def x 11)(->> x (+ 1) (> 2))"]]
      (is (empty? (lint! expr))))))

(deftest single-logical-operand-test
  (testing "and called with one arg is a warning"
    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>", :row 1, :col 1, :level :warning,
          :message "Single arg use of and always returns the arg itself"})
       (lint! "(and 1)" "--lang" lang))
      (assert-submaps
       '({:file "<stdin>", :row 1, :col 12, :level :warning,
          :message "Single arg use of and always returns the arg itself"})
       (lint! "(->> 1 foo and)" "--lang" lang))))

  (testing "or called with one arg is a warning"
    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>", :row 1, :col 1, :level :warning,
          :message "Single arg use of or always returns the arg itself"})
       (lint! "(or 1)" "--lang" lang))
      (assert-submaps
       '({:file "<stdin>", :row 1, :col 7, :level :warning,
          :message "Single arg use of or always returns the arg itself"})
       (lint! "(-> 1 (or))" "--lang" lang))))

  (doseq [lang ["clj" "cljs"]]
    (is (empty? (lint! "(and 1 2)" "--lang" lang)) "and with > 1 arg is ok")
    (is (empty? (lint! "(or 1 2 3)" "--lang" lang)) "or with > 1 arg is ok")))

(deftest reader-conditional-test
  (is (empty? (lint! "(let [scope *ns*]
                 (or (#{:default} scope)
                     #?(:clj (keyword? scope))))"
                     {:linters {:single-logical-operand {:level :warning}}}
                     "--lang" "cljc"))))
