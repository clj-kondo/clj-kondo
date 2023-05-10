(ns clj-kondo.plus-minus-one-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is testing]]))

(deftest plus-minus-one-test
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 11,
     :level :warning,
     :message "Prefer (inc x) over (+ 1 x)"}
    {:file "<stdin>",
     :row 1,
     :col 19,
     :level :warning,
     :message "Prefer (inc x) over (+ 1 x)"}
    {:file "<stdin>",
     :row 1,
     :col 27,
     :level :warning,
     :message "Prefer (dec x) over (- x 1)"}]
   (lint! "(def x 1) (+ 1 x) (+ x 1) (- x 1) (- 1 x)"
          {:linters {:plus-one {:level :warning}
                     :minus-one {:level :warning}}}))
  (is (empty? (lint! "(+ 1 2 3) (+ 2 1 3) (+ 2 3 1) (- 1 2) (- 2 1 2)"
                     {:linters {:plus-one {:level :warning}
                                :minus-one {:level :warning}}}))))
