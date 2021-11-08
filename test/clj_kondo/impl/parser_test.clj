(ns clj-kondo.impl.parser-test
  (:require [clj-kondo.impl.parser :as parser :refer [parse-string]]
            [clj-kondo.impl.rewrite-clj.reader :refer [*reader-exceptions*]]
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
  "Parse the source, which is expected to contain a syntax error, and return a
  sequence of error messages in the form [message line column] produced from
  rewrite-clj."
  [source]
  (let [ex (atom nil)
        token-exceptions (atom [])]
    (try (binding [*reader-exceptions* token-exceptions]
           (parse-string source))
         (catch Exception e (reset! ex e)))
    (let [^Exception e (or @ex (first @token-exceptions))
          _ (reset! ex nil)
          {:keys [findings line col]} (ex-data e)]
      (if findings
        (for [{:keys [row col message]} findings]
          [message row col])
        [[(.getMessage e) line col]]))))

(deftest parse-string-test
  ;; This test has every syntax error that can cause rewrite-clj to throw using
  ;; the `clj-kondo.impl.rewrite-clj.parser.utils/throw-reader` function.
  ;; This allows us to test for regressions when that function is refactored.
  (are [source messages] (= messages (parse-error source))
    "}" [["Unmatched bracket: unexpected }" 1 1]]
    "[" [["Found an opening [ with no matching ]" 1 1]
         ["Expected a ] to match [ from line 1" 1 2]]
    "[}"  [["Mismatched bracket: found an opening [ and a closing } on line 1" 1 1]
           ["Mismatched bracket: found an opening [ on line 1 and a closing }" 1 2]]
    "#"  [["Unexpected EOF." 1 2]]
    ":"  [["unexpected EOF while reading keyword." 1 2]]
    "\"" [["Unexpected EOF while reading string." 1 2]]
    "#?" [[":reader-macro node expects 1 value." 1 3]]
    "[1..1]" [["Invalid number: 1..1." 1 2]]
    "#:" [["Unexpected EOF." 1 3]]))

(deftest sexpr-test
  (is (= '(clojure.core/unquote (+ 1 2 3))
         (utils/sexpr (first (:children (utils/parse-string "`~(+ 1 2 3)"))))))
  (is (= '(clojure.core/unquote-splicing (+ 1 2 3))
         (utils/sexpr (first (:children (utils/parse-string "`~@(+ 1 2 3)")))))))

;;;; Scratch

(comment
  (t/run-tests)
  )
