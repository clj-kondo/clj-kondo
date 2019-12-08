(ns clj-kondo.unresolved-namespace-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.java.io :as io]
   [clojure.test :as t :refer [deftest is testing]]
   [clojure.edn :as edn]))

(deftest unresolved-namespace-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 2, :level :warning,
      :message "Unresolved namespace clojure.string. Are you missing a require?"})
   (lint! "(clojure.string/includes? \"foo\" \"o\")"))
  ;; avoiding false positives
  ;; TODO:
  #_(is (empty? (lint! (io/file "project.clj")))))
