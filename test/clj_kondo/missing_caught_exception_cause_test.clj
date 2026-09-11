(ns clj-kondo.missing-caught-exception-cause-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is]]))

(deftest missing-caught-exception-cause-test
  (let [config {:linters {:missing-caught-exception-cause {:level :warning}}}]
    (assert-submaps2
     '({:row 1
        :col 39
        :message "Pass the caught exception as the ex-info cause"})
     (lint! "(try (work) (catch Exception e (throw (ex-info \"Failed\" {}))))"
            config))
    (is (empty? (lint! "(try (work)
                         (catch Exception e
                           (throw (ex-info \"Failed\" {} e))))"
                       config)))
    (is (empty? (lint! "(try (work)
                         (catch Exception _
                           (throw (ex-info \"Replacement\" {}))))"
                       config)))
    (is (empty? (lint! "(let [ex-info replacement]
                         (try (work)
                           (catch Exception e
                             (throw (ex-info \"Replacement\" {})))))"
                       config)))
    (is (empty? (lint! "(try (work)
                         (catch Exception e
                           (throw (ex-info \"Failed\" {}))))"
                       {:linters {:missing-caught-exception-cause
                                  {:level :off}}})))))
