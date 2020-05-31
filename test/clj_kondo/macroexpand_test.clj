(ns clj-kondo.macroexpand-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest testing is]]))

(deftest macroexpand-test
  (assert-submaps
   '({:file "corpus/macroexpand.clj",
      :row 16, :col 7,
      :level :error,
      :message "Expected: number, received: keyword."}
     {:file "corpus/macroexpand.clj", :row 18, :col 1,
      :level :error, :message "Cannot call weird-macro with 0 arguments"})
   (lint! (io/file "corpus" "macroexpand.clj")
              {:linters {:type-mismatch {:level :error}}})))
