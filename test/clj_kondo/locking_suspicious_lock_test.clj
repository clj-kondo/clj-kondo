(ns clj-kondo.locking-suspicious-lock-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps assert-submaps2]]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]))

(deftest locking-suspicious-lock-test
  (assert-submaps2 '({:file "<stdin>",
                      :row 1,
                      :col 10,
                      :level :warning,
                      :message "Suspicious lock object: no body provided"})
                   (lint! "(locking (+ 1 2 3))"))
  (assert-submaps2 '({:file "<stdin>",
                      :row 1,
                      :col 10,
                      :level :warning,
                      :message "Suspicious lock object: object is local to locking scope"})
                   (lint! "(locking (Object.) (+ 1 2 3))"))
  (assert-submaps2 '({:file "<stdin>",
                      :row 1,
                      :col 10,
                      :level :warning,
                      :message "Suspicious lock object: object is local to locking scope"})
                   (lint! "(locking ::dude (+ 1 2 3))")))
