(ns clj-kondo.redundant-nesting-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps]]
            [clojure.test :refer [deftest is testing]]))

(deftest redundant-nesting-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 10, :level :warning, :message "Nested use of min is redunant"})
   (lint! "(min 5 2 (min 3 7))"))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 8, :level :warning, :message "Nested use of * is redunant"})
   (lint! "(* 3 4 (* 5 6))"))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 18, :level :warning, :message "Nested use of str is redunant"})
   (lint! "(str \"foo\" \"bar\" (str \"baz\" \"qux\"))"))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 21, :level :warning, :message "Nested use of concat is redunant"})
   (lint! "(concat [1 2] [3 4] (concat [5 6] [7 8]))")))
