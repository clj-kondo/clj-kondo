(ns clj-kondo.unresolved-symbol-test
  (:require
    [clj-kondo.test-utils :refer [lint! assert-submaps]]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.test :refer [deftest is testing]]))

(deftest unresolved-symbol-test
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 2,
      :level :error,
      :message "Unresolved symbol: x"})
   (lint! "(x)" "--config" "{:linters {:unresolved-symbol {:level :error}}}"))
  (testing "unresolved symbol is reported only once"
    (assert-submaps
     '({:file "<stdin>",
        :row 1,
        :col 2,
        :level :error,
        :message "Unresolved symbol: x"})
     (lint! "(x)(x)" "--config" "{:linters {:unresolved-symbol {:level :error}}}")))
  (testing "unresolved symbol is reported multiple times if configured"
    (assert-submaps
      '({:file "<stdin>",
         :row 1,
         :col 2,
         :level :error,
         :message "Unresolved symbol: x"}
        {:file "<stdin>",
         :row 1,
         :col 5,
         :level :error,
         :message "Unresolved symbol: x"} )
      (lint! "(x)(x)" "--config" "{:linters {:unresolved-symbol {:level :error}}}"
             {:linters {:unresolved-symbol {:report-duplicates true}}})))
  (assert-submaps
   '({:file "corpus/unresolved_symbol.clj",
      :row 11,
      :col 4,
      :level :error,
      :message "Unresolved symbol: unresolved-fn1"}
     {:file "corpus/unresolved_symbol.clj",
      :row 15,
      :col 1,
      :level :error,
      :message "clojure.set/join is called with 0 args but expects 2 or 3"}
     {:file "corpus/unresolved_symbol.clj",
      :row 18,
      :col 2,
      :level :error,
      :message "Unresolved symbol: foo"}
     {:file "corpus/unresolved_symbol.clj",
      :row 22,
      :col 1,
      :level :error,
      :message "unresolved-symbol2/bar is called with 1 arg but expects 0"})
   (lint! (io/file "corpus" "unresolved_symbol.clj")
          '{:linters {:unresolved-symbol {:level :error}}}))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 1,
      :level :error,
      :message "Unresolved symbol: x"})
   (lint! "x"
          '{:linters {:unresolved-symbol {:level :error}}}))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 20,
      :level :warning,
      :message "namespace clojure.pprint is required but never used"}
     {:file "<stdin>",
      :row 1,
      :col 49,
      :level :error,
      :message "Unresolved symbol: pprint"})
   (lint! "(ns foo (:require [clojure.pprint :as pprint])) pprint"
          '{:linters {:unresolved-symbol {:level :error}}}))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 20,
      :level :warning,
      :message "namespace clojure.pprint is required but never used"}
     {:file "<stdin>",
      :row 1,
      :col 50,
      :level :error,
      :message "Unresolved symbol: pprint"})
   (lint! "(ns foo (:require [clojure.pprint :as pprint])) (pprint)"
          '{:linters {:unresolved-symbol {:level :error}}}))
  (testing "slurp is unresolved in the cljs part of cljc"
    (assert-submaps
     '({:file "<stdin>",
        :row 1,
        :col 1,
        :level :error,
        :message "Unresolved symbol: slurp"})
     (lint! "slurp"
            '{:linters {:unresolved-symbol {:level :error}}}
            "--lang" "cljc")))
  (testing "metadata in ns macro is linted"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 25, :level :error, :message "Unresolved symbol: a"}
       {:file "<stdin>", :row 1, :col 30, :level :error, :message "clojure.core/inc is called with 0 args but expects 1"})
     (lint! "(ns foo \"docstring\" {:a a :b (inc)})"
            '{:linters {:unresolved-symbol {:level :error}}})))
  (testing "position"
    (assert-submaps
     '({:file "<stdin>", :row 2, :col 2, :level :error, :message "Unresolved symbol: split-arguments-string"})
     (lint!
      "(ns babashka.process-test (:require [clojure.test :as t :refer [deftest is]]))
(split-arguments-string)
(split-arguments-string)"
      '{:linters
        {:unused-namespace {:level :off}
         :unresolved-symbol {:level :error}}})))
  (testing "config is merged"
    (is (empty? (lint! "(ns foo (:require [clojure.test :as t])) (t/is (foo? (inc 1))) (t/is (bar? (inc 1)))"
                       '{:linters {:unresolved-symbol {:level :error
                                                       :exclude [(clojure.test/is [foo?])
                                                                 (clojure.test/is [bar?])]}}}))))
  ;; Preventing false positives
  (is (empty? (lint! "slurp"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(try 1 (catch Exception e e) (finally 3))"
                     {:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(defmulti foo (fn [_])) (defmethod foo :dude [_]) (foo 1)"
                     {:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(defonce foo (fn [_])) (foo 1)"
                     {:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(defmacro foo [] `(let [x# 1]))"
                     {:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(definline foo [] `(let [x# 1]) (foo))"
                     {:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(let [e (Exception.)] (.. e getCause getMessage))"
                     {:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "`(let [e# (Exception.)] (.. e# getCause getMessage))"
                     {:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "`~@(let [v nil] (resolve v))"
                     {:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "#inst \"2019\""
                     {:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(if-some [foo true] foo false)"
                     {:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(ns foo) (defn foo [_ _ _]) (foo x y z)"
                     '{:linters {:unresolved-symbol {:level :error
                                                     :exclude [(foo/foo [x y z])]}}})))
  (is (empty? (lint! "(defprotocol IFoo) IFoo"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(defrecord Foo []) Foo"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(deftype Foo []) Foo"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "Object BigDecimal"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(ns foo (:import [my.package Foo])) Foo"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(ns foo (:import (my.package Foo))) Foo"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(ns foo (:import my.package.Foo)) Foo"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(dotimes [_ 10] (println \"hello\"))"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(let [{{:keys [:a]} :stats} {:stats {:a 1}}] a)"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "java.math.BitSieve"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "Class Object Cloneable NoSuchFieldError String"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(let [{:keys [:as]} {:as 1}] as)"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(as-> 1 x)"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(let [x 1 {:keys [:a] :or {a x}} {:a 1}])"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(defmacro foo [] &env &form)"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(let [a (into-array [])] (areduce a i ret 0 (+ ret (aget a i))))"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(this-as x [x x x])"
                     '{:linters {:unresolved-symbol {:level :error}}}
                     "--lang" "cljs")))
  (is (empty? (lint! "(as-> 10 x (inc x) (inc x))"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "((memfn ^String substring start end) \"foo\" 0 1)"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(goog-define foo \"default\")"
                     '{:linters {:unresolved-symbol {:level :error}}}
                     "--lang" "cljs")))
  (is (empty? (lint! "(definterface Foo (foo [])) Foo"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "Var Namespace LazySeq UUID"
                     '{:linters {:unresolved-symbol {:level :error}}}
                     "--lang" "cljs")))
  (is (empty? (lint! "(defn str-to-str [s] {:post [(string? %)]} s)"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(fn [s] {:post [(string? %)]} s)"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (:findings
               (edn/read-string
                (with-out-str
                  (lint! "#(inc %4)"
                         '{:linters {:unused-binding {:level :error}}
                           :output {:format :edn}}))))))
  (is (empty? (lint! "(ns foo (:import [java.util.regex Pattern])) Pattern/compile"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  ;; although this isn't correct at run-time, preventing a namespace or class
  ;; symbol from being reported as unresolved is generally better
  (is (empty? (lint! "(ns foo (:require [clojure.core])) clojure.core"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(ns foo (:require [clojure.string :refer :all]))
                      join starts-with? ends-with?"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(when-first [a [1 2 3]] a)"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(ns foo (:import [goog.date UtcDateTime]))
                      (defn utc [x] (UtcDateTime.fromTimestamp x))"
                     '{:linters {:unresolved-symbol {:level :error}}}
                     "--lang" "cljs")))
  (is (empty? (lint! "(import '[java.foo Bar Baz]) Bar Baz"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "
(defmulti example first)

(defmethod example :x
  f
  ([] :foo)
  ([[_ v]]
   [(f) :bar]))"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "
(ns app.core
  (:require [slingshot.slingshot]))

(slingshot.slingshot/try+
 x
 (:foo x))

" '{:linters {:unresolved-symbol {:exclude [(slingshot.slingshot/try+)]}}})))
  (is (empty? (lint! "(def ^name.fraser.neil.plaintext.diff_match_patch dmp (diff_match_patch.))"
                     '{:linters {:unresolved-symbol {:level :error}}}))))

(deftest cljs-property-access-test
  (is (empty? (lint! "(defn editable? [coll] (satisfies? cljs.core.IEditableCollection coll))"
                     '{:linters {:unresolved-symbol {:level :error}}}))))

(deftest unresolved-dot-symbol-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 11, :level :error, :message "Unresolved symbol: ."})
   (lint! "(or false . true)"
          '{:linters {:unresolved-symbol {:level :error}}})))
