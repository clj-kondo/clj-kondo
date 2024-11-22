(ns clj-kondo.redundant-nested-call-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps2]]
            [clojure.test :refer [deftest is testing]]))

(deftest redundant-nesting-test
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 10, :level :info, :message "Redundant nested call: min"})
   (lint! "(min 5 2 (min 3 7))"))
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 8, :level :info, :message "Redundant nested call: *"})
   (lint! "(* 3 4 (* 5 6))"))
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 18, :level :info, :message "Redundant nested call: str"})
   (lint! "(str \"foo\" \"bar\" (str \"baz\" \"qux\"))"))
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 15, :level :info, :message "Redundant nested call: merge"})
   (lint! "(merge {:a 1} (merge {:b 2} {:b 3}))"))
  (is
   (empty?
    (lint! "#_:clj-kondo/ignore (str [1 2] [3 4] (str [5 6] [7 8]))")))
  (is
   (empty?
    (lint! "(ns foo) (str [1 2] [3 4] (str [5 6] [7 8]))"
           '{:config-in-ns {foo {:linters {:redundant-nested-call {:level :off}}}}})))
  (is
   (empty?
    (lint! "(ns foo) (str [1 2] [3 4] (str [5 6] [7 8]))"
           '{:config-in-call {clojure.core/str {:linters {:redundant-nested-call {:level :off}}}}})))
  (is
   (empty?
    (lint! "(-> {:a 1} (merge {:b 2}) (merge {:b 3}))"))))
