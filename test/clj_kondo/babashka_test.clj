(ns clj-kondo.babashka-test
  (:require
   [clj-kondo.test-utils :refer
    [lint! assert-submaps]]
   [clojure.java.io :as io]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest format-test
  (assert-submaps
   '({:file "corpus/babashka_script.clj", :row 16, :col 9, :level :error, :message "Unresolved symbol: x"})
   (lint! (io/file "corpus" "babashka_script.clj")
          {:linters {:unresolved-symbol {:level :error}}})))
