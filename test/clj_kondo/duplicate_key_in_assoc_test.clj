(ns clj-kondo.duplicate-key-in-assoc-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is testing]]))

(deftest duplicate-key-in-assoc-test
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 11,
     :level :warning,
     :message "Duplicate key in assoc: :x"}]
   (lint! "(assoc {} :x 1 :x 2)"))
  (assert-submaps2
   []
   (lint! "(defn side-effecting! []) (assoc {} (side-effecting!) 1 (side-effecting!) 2)"))
  (assert-submaps2
   [{:file "<stdin>",
     :row 3,
     :col 13,
     :level :warning,
     :message "Duplicate key in assoc: x"}]
   (lint! "(defn side-effecting! [])
(let [x (side-effecting!)]
  (assoc {} x 1 x 2))")))
