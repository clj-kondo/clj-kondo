(ns clj-kondo.redundant-expression-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :refer [deftest is testing]]))

(deftest shadowed-var-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 5, :level :warning, :message "Redundant expression: 1"})
   (lint! "(do 1 2) "))
  (is (empty?
       (lint! "(fn [] (:foo {}))")))
  (is (empty?
       (lint! "(map
 (fn [{:keys [:vars :proxied-namespaces]}]
   (assoc {:vars vars}
          :proxied-namespaces proxied-namespaces))
 [])")))
  (is (empty? (lint! "
(let [f #(+ % %2)] (f 1 2))"))))
