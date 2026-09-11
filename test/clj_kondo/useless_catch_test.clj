(ns clj-kondo.useless-catch-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is]]))

(deftest useless-catch-test
  (let [config {:linters {:useless-catch {:level :warning}}}]
    (assert-submaps2
     '({:row 1
        :col 13
        :message "Catch clause only rethrows the caught exception"})
     (lint! "(try (work) (catch Exception e (throw e)))" config))
    (is (empty? (lint! "(try (work)
                         (catch java.io.IOException e (throw e))
                         (catch Exception e (handle e)))"
                       config)))
    (is (empty? (lint! "(try (work) (catch Exception e (log e) (throw e)))"
                       config)))
    (is (empty? (lint! "(try (work) (catch Exception e (throw e)))"
                       {:linters {:useless-catch {:level :off}}})))))
