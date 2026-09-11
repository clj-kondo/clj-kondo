(ns clj-kondo.duplicate-cond-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is]]))

(def config
  {:linters {:duplicate-cond-test {:level :warning}}})

(deftest duplicate-cond-test
  (assert-submaps2
   '({:row 1 :col 18 :message "Duplicate cond test"})
   (lint! "(cond (odd? x) 1 (odd? x) 2 :else 3)" config))
  (assert-submaps2
   '({:row 1 :col 16 :message "Duplicate cond test"})
   (lint! "(cond ready? 1 ready? 2)" config))
  (is (empty? (lint! "(cond (odd? x) 1 (even? x) 2)" config)))
  (is (empty? (lint! "(cond true 1 true 2)"
                     {:linters {:duplicate-cond-test {:level :warning}
                                :constant-condition {:level :off}
                                :cond-else {:level :off}}})))
  (is (empty? (lint! "(cond (odd? x) 1 (odd? x) 2)"
                     {:linters {:duplicate-cond-test {:level :off}}}))))
