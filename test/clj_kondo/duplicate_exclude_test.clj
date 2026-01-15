(ns clj-kondo.duplicate-exclude-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is testing]]))

(deftest duplicate-exclude-test
  (testing "duplicate exclude in refer-clojure"
    (assert-submaps2
     [{:row 1, :col 39, :level :warning, :message "Duplicate exclude: map"}]
     (lint! "(ns foo (:refer-clojure :exclude [map map]))"
            "{:linters {:unused-excluded-var {:level :off}}}")))

  (testing "multiple duplicate excludes in refer-clojure"
    (assert-submaps2
     [{:row 1, :col 46, :level :warning, :message "Duplicate exclude: map"}
      {:row 1, :col 57, :level :warning, :message "Duplicate exclude: filter"}]
     (lint! "(ns foo (:refer-clojure :exclude [map filter map reduce filter]))"
            "{:linters {:unused-excluded-var {:level :off}}}")))

  (testing "no warning when excludes are different"
    (is (empty? (lint! "(ns foo (:refer-clojure :exclude [map filter]))"
                       "{:linters {:unused-excluded-var {:level :off}}}"))))

  (testing "duplicate exclude in require"
    (assert-submaps2
     [{:row 1, :col 48, :level :warning, :message "Duplicate exclude: union"}]
     (lint! "(ns foo (:require [clojure.set :exclude [union union]]))")))

  (testing "multiple duplicate excludes in require"
    (assert-submaps2
     [{:row 1, :col 48, :level :warning, :message "Duplicate exclude: union"}
      {:row 1, :col 67, :level :warning, :message "Duplicate exclude: intersection"}]
     (lint! "(ns foo (:require [clojure.set :exclude [union union intersection intersection]]))")))

  (testing "no warning when excludes in require are different"
    (is (empty? (lint! "(ns foo (:require [clojure.set :exclude [union intersection]]))"))))

  (testing "duplicate exclude with ignored symbol"
    (is (empty? (lint! "(ns foo (:refer-clojure :exclude [#_:clj-kondo/ignore map map]))"
                       "{:linters {:unused-excluded-var {:level :off}}}")))
    (is (empty? (lint! "(ns foo (:refer-clojure :exclude [map #_:clj-kondo/ignore map]))"
                       "{:linters {:unused-excluded-var {:level :off}}}"))))

  (testing "can be disabled"
    (is (empty? (lint! "(ns foo (:refer-clojure :exclude [map map]))"
                       "{:linters {:duplicate-exclude {:level :off}
                                   :unused-excluded-var {:level :off}}}")))))
