(ns clj-kondo.single-arity-comparison-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest single-arity-comparison-test
  (testing "test full linting error for single arity comparison in clojure"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "single arity use of clojure.core/= is constantly true"})
     (lint! "(= 1)")))

  (testing "test full linting error for single arity comparison in cljs"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "single arity use of cljs.core/not= is constantly false"})
     (lint! "(not= 1)" "--lang" "cljs")))

  (testing "test linting comparison operators with single arity"
    (doseq [lang ["clojure" "cljs"]
            op ["=" ">" "<" ">=" "<=" "=="]
            :let [errors (lint! (str "(" op " 1)") "--lang" lang)]]
      (is (= (count errors) 1))
      (is (= (str "single arity use of " lang ".core/" op " is constantly true"
              (-> errors
                  (first)
                  :message))))))

  (testing "test linting single arity comparison in threading macros"
    (is (seq (lint! "(-> 10 (>))")))
    (is (seq (lint! "(->> 10 (>))")))
    (is (seq (lint! "(def x 11)(->> x (+ 1) (>))"))))

  (testing "test linting single arity comparison for valid expressions"
    (doseq [expr ["(= 1 2)"
                  "(= (== 1 2) false)"
                  "(-> 10 (> 20))"
                  "(->> 10 (< 20))"
                  "(def x 11)(->> x (+ 1) (> 2))"]]
      (is (empty? (lint! expr))))))
