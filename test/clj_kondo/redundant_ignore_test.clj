(ns clj-kondo.redundant-ignore-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps assert-submaps2]]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]))

(def config {:linters {:redundant-ignore {:level :warning}}})

(deftest redundant-ignore-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 3, :level :warning, :message "Redundant ignore"})
   (lint! "#_:clj-kondo/ignore (+ 1 2 3)" config)))

(deftest redundant-ignore-unused-private-var-test
  (assert-submaps
   []
   (lint! "#_{:clj-kondo/ignore [:unused-private-var]}
(defn- -debug [& strs]
  (.println System/err
            (with-out-str
              (apply println strs))))"
          config)))
