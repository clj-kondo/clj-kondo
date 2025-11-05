(ns clj-kondo.duplicate-key-args-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is testing]]))

(deftest duplicate-key-args-test
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 16,
     :level :warning,
     :message "Duplicate key args for assoc: :x"}]
   (lint! "(assoc {} :x 1 :x 2)"))
  (assert-submaps2
   []
   (lint! "(defn side-effecting! []) (assoc {} (side-effecting!) 1 (side-effecting!) 2)"))
  (assert-submaps2
   [{:file "<stdin>",
     :row 3,
     :col 17,
     :level :warning,
     :message "Duplicate key args for assoc: x"}]
   (lint! "(defn side-effecting! [])
(let [x (side-effecting!)]
  (assoc {} x 1 x 2))"))
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 29,
     :level :warning,
     :message "Duplicate key args for assoc!: :x"}]
   (lint! "(assoc! (transient {}) :x 1 :x 2 :x 3)"))
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 15,
     :level :warning,
     :message "Duplicate key args for dissoc: :x"}]
   (lint! "(dissoc {} :x :x)"))
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 28,
     :level :warning,
     :message "Duplicate key args for dissoc!: :y"}]
   (lint! "(dissoc! (transient {}) :y :y)"))
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 16,
     :level :warning,
     :message "Duplicate key args for hash-map: :x"}]
   (lint! "(hash-map :x 1 :x 2)"))
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 17,
     :level :warning,
     :message "Duplicate key args for array-map: :x"}]
   (lint! "(array-map :x 1 :x 2)"))
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 18,
     :level :warning,
     :message "Duplicate key args for sorted-map: :x"}]
   (lint! "(sorted-map :x 1 :x 2)"))
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 23,
     :level :warning,
     :message "Duplicate key args for sorted-map-by: :x"}]
   (lint! "(sorted-map-by > :x 1 :x 2)"))
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 20,
     :level :warning,
     :message "Duplicate key args for struct-map: :x"}]
   (lint! "(struct-map s :x 1 :x 2)"))
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 25,
     :level :warning,
     :message "Duplicate key args for disj: :x"}]
   (lint! "(disj #{:x :y :z} :x :y :x)"))
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 38,
     :level :warning,
     :message "Duplicate key args for disj!: :x"}]
   (lint! "(disj! (transient #{:x :y :z}) :x :y :x)"))
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 22,
     :level :warning,
     :message "Duplicate key args for create-struct: :a"}]
   (lint! "(create-struct :a :b :a)"))
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 22,
     :level :warning,
     :message "Duplicate key args for defstruct: :a"}]
   (lint! "(defstruct foo :a :b :a)"))
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 17,
     :level :warning,
     :message "Duplicate key args for hash-set: :a"}]
   (lint! "(hash-set :a :b :a)"))
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 19,
     :level :warning,
     :message "Duplicate key args for sorted-set: :a"}]
   (lint! "(sorted-set :a :b :a)"))
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 39,
     :level :warning,
     :message "Duplicate key args for sorted-set-by: :a"}]
   (lint! "(sorted-set-by #(compare %2 %1) :a :b :a)")))
