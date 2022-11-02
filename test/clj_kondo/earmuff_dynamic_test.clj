(ns clj-kondo.earmuff-dynamic-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps]]
            [clojure.test :refer [deftest is testing]]))

(deftest earmuffed-but-not-dynamic-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 6, :level :warning, :message "Var has earmuffed name but is not declared dynamic: *foo*"})
   (lint! "(def *foo* 1)" {:linters {:earmuffed-var-not-dynamic {:level :warning}}}))

  (is (empty?
       (lint! "(def *foo* 1)" {:linters {:earmuffed-var-not-dynamic {:level :off}}})))
  (is (empty?
       (lint! "#_:clj-kondo/ignore (def *foo* 1)"
              {:linters {:earmuffed-var-not-dynamic {:level :warning}}})))
  (is (empty?
       (lint! "(def #_:clj-kondo/ignore *foo* 1)"
              {:linters {:earmuffed-var-not-dynamic {:level :warning}}}))))

(deftest dynamic-but-not-earmuffed-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 16, :level :warning, :message "Var is declared dynamic but name is not earmuffed: foo"})
   (lint! "(def ^:dynamic foo)" {:linters {:dynamic-var-not-earmuffed {:level :warning}}}))

  (is (empty?
       (lint! "(def ^:dynamic foo 1)" {:linters {:dynamic-var-not-earmuffed {:level :off}}})))
  (is (empty?
       (lint! "#_:clj-kondo/ignore (def ^:dynamic foo 1)"
              {:linters {:dynamic-var-not-earmuffed {:level :warning}}})))
  (is (empty?
       (lint! "(def ^:dynamic #_:clj-kondo/ignore foo 1)"
              {:linters {:dynamic-var-not-earmuffed {:level :warning}}}))))
