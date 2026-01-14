(ns clj-kondo.duplicate-refer-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is testing]]))

(deftest duplicate-refer-test
  (testing "duplicate refer in ns require"
    (assert-submaps2
     '[{:row 2, :col 40, :level :warning, :message "Duplicate refer: union"}]
     (lint! "(ns foo
  (:require [clojure.set :refer [union union]]))
(union #{1} #{2})"
            "{:linters {:unused-namespace {:level :off}
                        :unused-referred-var {:level :off}}}")))

  (testing "multiple duplicate refers"
    (assert-submaps2
     '[{:row 2, :col 40, :level :warning, :message "Duplicate refer: union"}
       {:row 2, :col 59, :level :warning, :message "Duplicate refer: intersection"}]
     (lint! "(ns foo
  (:require [clojure.set :refer [union union intersection intersection]]))"
            "{:linters {:unused-namespace {:level :off}
                        :unused-referred-var {:level :off}}}")))

  (testing "no warning when refers are different"
    (is (empty? (lint! "(ns foo
  (:require [clojure.set :refer [union intersection]]))"
                       "{:linters {:unused-namespace {:level :off}
                                   :unused-referred-var {:level :off}}}"))))

  (testing "duplicate refer in standalone require"
    (assert-submaps2
     '[{:row 1, :col 38, :level :warning, :message "Duplicate refer: union"}]
     (lint! "(require '[clojure.set :refer [union union]])"
            "{:linters {:unused-namespace {:level :off}
                        :unused-referred-var {:level :off}}}")))

  (testing "duplicate refer-macros"
    (assert-submaps2
     '[{:row 2, :col 47, :level :warning, :message "Duplicate refer: defn"}]
     (lint! "(ns foo
  (:require [clojure.core :refer-macros [defn defn]]))"
            "{:linters {:unused-namespace {:level :off}
                        :unused-referred-var {:level :off}}}"))))
