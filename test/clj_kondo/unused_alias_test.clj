(ns clj-kondo.unused-alias-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps2]]
   [clojure.test :refer [deftest testing is]]))

(deftest unused-import-test
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 34, :level :warning, :message "Unused alias: b"})
   (lint! "(ns foo (:require [bar :as-alias b]))")))
