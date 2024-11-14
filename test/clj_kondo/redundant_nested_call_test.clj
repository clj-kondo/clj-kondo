(ns clj-kondo.redundant-nested-call-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps]]
            [clojure.test :refer [deftest is testing]]))

(deftest redundant-nesting-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 10, :level :warning, :message "Redundant nested call: min"})
   (lint! "(min 5 2 (min 3 7))"))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 8, :level :warning, :message "Redundant nested call: *"})
   (lint! "(* 3 4 (* 5 6))"))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 18, :level :warning, :message "Redundant nested call: str"})
   (lint! "(str \"foo\" \"bar\" (str \"baz\" \"qux\"))"))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 21, :level :warning, :message "Redundant nested call: concat"})
   (lint! "(concat [1 2] [3 4] (concat [5 6] [7 8]))")))
