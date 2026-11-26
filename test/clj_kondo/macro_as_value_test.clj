(ns clj-kondo.macro-as-value-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is testing]]))

(def msg "Can't take value of a macro: and")

(deftest macro-as-value-test
  (testing "a macro passed to map warns"
    (assert-submaps2
     [{:row 1 :col 6 :level :error :message msg}]
     (lint! "(map and [1 2])" {:linters {:single-logical-operand {:level :off}}})))
  (testing "a macro passed to a known function warns"
    (assert-submaps2
     [{:row 1 :col 24 :message msg}]
     (lint! "(defn f [g] g) (f (inc and))")))
  (testing "a macro in a map literal argument warns"
    (assert-submaps2
     [{:row 1 :col 23 :message msg}]
     (lint! "(defn f [g] g) (f {:a and})")))
  (testing "a macro under when warns"
    (assert-submaps2
     [{:row 1 :col 17 :message msg}]
     (lint! "(when true (inc and))")))
  (testing "a macro under a :macroexpand hook warns"
    (assert-submaps2
     [{:row 1 :col 69 :message msg}]
     (lint! "(ns foo) (defmacro my-do [& body] `(do ~@body)) (inc 1) (my-do (inc and))"
            '{:hooks {:__dangerously-allow-string-hooks__ true
                      :macroexpand {foo/my-do "(defmacro my-do [& body] `(do ~@body))"}}})))
  (testing "a macro under a :lint-as clojure.core/let warns"
    (assert-submaps2
     [{:row 1 :col 56 :message msg}]
     (lint! "(ns foo) (defmacro my-let [& body]) (my-let [x 1] (inc and))"
            '{:lint-as {foo/my-let clojure.core/let}})))
  (testing "a macro as arg of a keyword, local, fn literal or anonymous fn call warns"
    (assert-submaps2
     [{:row 1 :col 5 :message msg}
      {:row 1 :col 31 :message msg}
      {:row 1 :col 47 :message msg}
      {:row 1 :col 64 :message msg}]
     (lint! "(:a and) (let [f identity] (f and)) (#(inc %) and) ((fn [x] x) and)")))
  (testing "a macro as arg of an interop call warns"
    (assert-submaps2
     [{:row 1 :col 12 :message msg}
      {:row 1 :col 26 :message msg}
      {:row 1 :col 43 :message msg}]
     (lint! "(.equals 1 and) (String. and) (new String and)")))
  (testing "a macro in cljs warns"
    (assert-submaps2
     [{:row 1 :col 6 :message msg}]
     (lint! "(map and [1 2])" {:linters {:single-logical-operand {:level :off}}} "--lang" "cljs")))
  (testing "a macro in cljc warns once"
    (assert-submaps2
     [{:row 1 :col 6 :message msg}]
     (lint! "(map and [1 2])" {:linters {:single-logical-operand {:level :off}}} "--lang" "cljc")))
  (testing "the message names an aliased macro by its alias"
    (assert-submaps2
     [{:row 1 :col 68 :message "Can't take value of a macro: b/m"}]
     (lint! "(ns bar) (defmacro m []) (ns foo (:require [bar :as b])) (identity b/m)")))
  (testing "a cljs var that is both a function and a macro does not warn"
    (is (empty? (lint! "(map + [1 2]) (map str [1 2])" "--lang" "cljs"))))
  (testing "a special form or a cljs field access on a macro does not warn"
    (is (empty? (lint! "(fn [] (:a var))" {:linters {:unresolved-symbol {:level :off}}})))
    (is (empty? (lint! "(set! (. defn -cljs$lang$macro) true)" "--lang" "cljs"))))
  (testing "#'and, (var and) and `and do not warn"
    (is (empty? (lint! "#'and (meta #'and) (var and) (list `and 1)"))))
  (testing "(var and) with a local named var does not warn"
    (is (empty? (lint! "(fn [{:keys [var]}] [var (var and)])"))))
  (testing "a local named and does not warn"
    (is (empty? (lint! "(let [and 1] (inc and))"))))
  (testing "a macro under an unknown macro does not warn"
    (is (empty? (lint! "(defmacro my-do [& body] `(do ~@body)) (my-do (inc and))"))))
  (testing "a macro as arg of a macro or unresolved call does not warn"
    (is (empty? (lint! "(defmacro m [& body]) (m and) (my-unknown-call and)"
                       {:linters {:unresolved-symbol {:level :off}}}))))
  (testing ":level :off disables the linter"
    (is (empty? (lint! "(defn f [g] g) (f and)" {:linters {:macro-as-value {:level :off}}})))))
