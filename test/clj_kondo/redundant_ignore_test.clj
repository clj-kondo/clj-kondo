(ns clj-kondo.redundant-ignore-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps2]]
            [clojure.test :refer [deftest is testing]]))

(def config {:linters {:redundant-ignore {:level :warning}}})

(deftest redundant-ignore-test
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 3, :level :warning, :message "Redundant ignore"})
   (lint! "#_:clj-kondo/ignore (+ 1 2 3)" config)))

(deftest redundant-ignore-unused-private-var-test
  (assert-submaps2
   []
   (lint! "#_{:clj-kondo/ignore [:unused-private-var]}
(defn- -debug [& strs]
  (.println System/err
            (with-out-str
              (apply println strs))))"
          config)))

(deftest redundant-ignore-exclude-test
  (is (empty?
       (lint! "#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var :unused-binding]} (defn foo [])"
              (assoc-in config [:linters :redundant-ignore :exclude] [:clojure-lsp/unused-public-var])))))
