(ns clj-kondo.impl.parser-test
  (:require [clj-kondo.impl.parser :as parser :refer [parse-string]]
            [clj-kondo.impl.utils :as utils]
            [clojure.test :as t :refer [deftest is are]]))

(deftest omit-unevals-test
  (is (zero? (count (:children (parse-string "#_#_1 2"))))))

(deftest namespaced-maps-test
  (is (= '#:it{:a 1} (utils/sexpr (utils/parse-string "#::it {:a 1}"))))
  (is (= '#:it{:a #:it{:a 1}} (utils/sexpr (utils/parse-string "#::it {:a #::it{:a 1}}"))))
  (is (= '#:__current-ns__{:a 1} (utils/sexpr (utils/parse-string "#::{:a 1}")))))

(deftest nan-test
  (is (= true (Double/isNaN (utils/sexpr (utils/parse-string "##NaN"))))))

(deftest inf-test
  (is (= true (let [thing (utils/sexpr (utils/parse-string "##Inf"))]
                (and (Double/isInfinite thing)
                     (< 0 thing)))))
  (is (= true (let [thing (utils/sexpr (utils/parse-string "##-Inf"))]
                (and (Double/isInfinite thing)
                     (< thing 0))))))

(defn- parse-error
  "Parse the source, which is expected to contain a syntax error, and return the
  error message produced from rewrite-clj."
  [source]
  (try
    (parse-string source)
    nil
    (catch Exception e
      (.getMessage e))))

(deftest parse-string-test
  ;; This test has every syntax error that can cause rewrite-clj to throw using
  ;; the `clj-kondo.impl.rewrite-clj.parser.utils/throw-reader` function.
  ;; This allows us to test for regressions when that function is refactored.
  (are [source message] (= message (parse-error source))
    "[\n" "Unexpected EOF. [at line 1, column 2]"
    "[" "Unexpected EOF. [at line 1, column 2]"
    "[}" "Unmatched delimiter: } [at line 1, column 2]"
    "#" "Unexpected EOF. [at line 1, column 2]"
    ":" "unexpected EOF while reading keyword. [at line 1, column 2]"
    "\"" "Unexpected EOF while reading string. [at line 1, column 2]"
    "#?" ":reader-macro node expects 1 value. [at line 1, column 3]"
    "#:" "Unexpected EOF. [at line 1, column 3]"))

;;;; Scratch

(comment
  (t/run-tests)
  )
