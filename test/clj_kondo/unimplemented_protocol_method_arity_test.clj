(ns clj-kondo.unimplemented-protocol-method-arity-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps2]]
            [clojure.test :refer [deftest is testing]]))

(deftest deftype-wrong-arity-test
  (testing "deftype method implemented with wrong arity"
    (assert-submaps2
     '({:file "<stdin>", :row 9, :col 4, :level :warning,
        :message "Protocol method bar is implemented with arity 1, expected one of: (2)"}
       {:file "<stdin>", :row 11, :col 4, :level :warning,
        :message "Protocol method baz is implemented with arity 4, expected one of: (1 2 3)"})
     (lint! "(ns test.foo)

(defprotocol AProtocol
  (bar [a b])
  (baz [a] [a b] [a b c]))

(deftype Foo [a b c]
  AProtocol
  (bar [x] a)
  (baz [x] b)
  (baz [x y z w] (+ a b c)))"))))

(deftest defrecord-wrong-arity-test
  (testing "defrecord method implemented with wrong arity"
    (assert-submaps2
     '({:file "<stdin>", :row 9, :col 4, :level :warning,
        :message "Protocol method foo is implemented with arity 2, expected one of: (1)"}
       {:file "<stdin>", :row 11, :col 4, :level :warning,
        :message "Protocol method bar is implemented with arity 3, expected one of: (1 2)"})
     (lint! "(ns test.foo)

(defprotocol P
  (foo [a])
  (bar [a] [a b]))

(defrecord R []
  P
  (foo [x y] :wrong)
  (bar [x] :ok)
  (bar [x y z] :wrong))"))))

(deftest reify-wrong-arity-test
  (testing "reify method implemented with wrong arity"
    (assert-submaps2
     '({:file "<stdin>", :row 8, :col 4, :level :warning,
        :message "Protocol method foo is implemented with arity 2, expected one of: (1)"})
     (lint! "(ns test.foo)

(defprotocol P
  (foo [a])
  (bar [a] [a b]))

(reify P
  (foo [this extra] :wrong)
  (bar [this] :ok)
  (bar [this x] :ok))"))))

(deftest reify-inside-let-test
  (testing "reify inside a let with wrong arity"
    (assert-submaps2
     '({:file "<stdin>", :row 10, :col 6, :level :warning,
        :message "Protocol method bar is implemented with arity 3, expected one of: (1 2)"})
     (lint! "(ns test.foo)

(defprotocol P
  (foo [a])
  (bar [a] [a b]))

(let [x 42]
  (reify P
    (foo [this] x)
    (bar [this a b] x)))"))))

(deftest extend-protocol-wrong-arity-test
  (testing "extend-protocol method implemented with wrong arity"
    (assert-submaps2
     '({:file "<stdin>", :row 9, :col 4, :level :warning,
        :message "Protocol method foo is implemented with arity 2, expected one of: (1)"})
     (lint! "(ns test.foo)

(defprotocol P
  (foo [a])
  (bar [a] [a b]))

(extend-protocol P
  String
  (foo [x y] :wrong)
  (bar [x] :ok))"))))

(deftest extend-type-wrong-arity-test
  (testing "extend-type method implemented with wrong arity"
    (assert-submaps2
     '({:file "<stdin>", :row 10, :col 4, :level :warning,
        :message "Protocol method bar is implemented with arity 3, expected one of: (1 2)"})
     (lint! "(ns test.foo)

(defprotocol P
  (foo [a])
  (bar [a] [a b]))

(extend-type Number
  P
  (foo [x] :ok)
  (bar [x y z] :wrong))"))))

(deftest no-warn-correct-arities-test
  (testing "no warning when all arities are correct"
    (is (empty?
         (lint! "(ns test.foo)

(defprotocol P
  (foo [a])
  (bar [a] [a b] [a b c]))

(defrecord R []
  P
  (foo [x] :ok)
  (bar [x] :ok)
  (bar [x y] :ok)
  (bar [x y z] :ok))

(deftype T []
  P
  (foo [x] :ok)
  (bar [x] :ok))

(reify P
  (foo [this] :ok)
  (bar [this] :ok)
  (bar [this x] :ok))

(extend-protocol P
  String
  (foo [x] :ok)
  (bar [x] :ok)
  (bar [x y] :ok))

(extend-type Number
  P
  (foo [x] :ok)
  (bar [x] :ok))")))))

(deftest definterface-wrong-arity-test
  (testing "definterface method implemented with wrong arity"
    (assert-submaps2
     '({:level :warning,
        :message "Protocol method foo is implemented with arity 1, expected one of: (2)"})
     (lint! "(ns test.foo)

(definterface IFoo
  (foo [x]))

(deftype T [a]
  IFoo
  (foo [this] :wrong))"))))

(deftest definterface-correct-arity-test
  (testing "no warning when definterface method has correct arity"
    (is (empty?
         (lint! "(ns test.foo)

(definterface IFoo
  (foo [x])
  (bar []))

(deftype T [a]
  IFoo
  (foo [this x] a)
  (bar [this] a))")))))

(deftest config-disabled-test
  (testing "linter can be disabled via config"
    (is (empty?
         (lint! "(ns test.foo)

(defprotocol P
  (foo [a]))

(deftype T []
  P
  (foo [x y] :wrong-arity))"
               {:linters {:unimplemented-protocol-method-arity {:level :off}}})))))

(deftest config-in-ns-test
  (testing "linter can be disabled via config-in-ns"
    (is (empty?
         (lint! "(ns repro)

(defprotocol P
  (foo [a]))

(deftype T []
  P
  (foo [x y] :wrong-arity))"
               '{:config-in-ns {repro {:linters {:unimplemented-protocol-method-arity {:level :off}}}}})))))
