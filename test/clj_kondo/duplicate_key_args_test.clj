(ns clj-kondo.duplicate-key-args-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is testing]]))

(deftest duplicate-key-args-test
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 11,
     :level :warning,
     :message "Duplicate key args for assoc: :x"}]
   (lint! "(assoc {} :x 1 :x 2)"))
  (assert-submaps2
   []
   (lint! "(defn side-effecting! []) (assoc {} (side-effecting!) 1 (side-effecting!) 2)"))
  (assert-submaps2
   [{:file "<stdin>",
     :row 3,
     :col 13,
     :level :warning,
     :message "Duplicate key args for assoc: x"}]
   (lint! "(defn side-effecting! [])
(let [x (side-effecting!)]
  (assoc {} x 1 x 2))"))
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 24,
     :level :warning,
     :message "Duplicate key args for assoc!: :x"}]
   (lint! "(assoc! (transient {}) :x 1 :x 2)"))
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 12,
     :level :warning,
     :message "Duplicate key args for dissoc: :x"}]
   (lint! "(dissoc {} :x :x)"))
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 25,
     :level :warning,
     :message "Duplicate key args for dissoc!: :y"}]
   (lint! "(dissoc! (transient {}) :y :y)")))
