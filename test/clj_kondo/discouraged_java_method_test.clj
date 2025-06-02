(ns clj-kondo.discouraged-java-method-test
  (:require
   [clj-kondo.test-utils :as tu :refer [lint! assert-submaps2]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest discouraged-java-method-test
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 15,
     :level :warning,
     :message "Don't use System/exit directly"}
    {:file "<stdin>",
     :row 1,
     :col 32,
     :level :warning,
     :message "Don't use System/exit directly"}]
   (lint! "(defn foo [x] (System/exit x)) System/exit #_:clj-kondo/ignore System/exit"
          '{:linters  {:discouraged-java-method {java.lang.System {exit {:message "Don't use System/exit directly"}}}}})))
