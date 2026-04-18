(ns clj-kondo.protocol-method-arity-mismatch-test
  (:require [babashka.fs :as fs]
            [clj-kondo.test-utils :refer [lint! assert-submaps2]]
            [clojure.test :refer [deftest is testing]]))

(deftest deftype-wrong-arity-test
  (testing "deftype method implemented with wrong arity"
    (assert-submaps2
     '({:file "<stdin>", :row 9, :col 4, :level :warning,
        :message "Protocol method bar is implemented with arity 1 but expects 2"}
       {:file "<stdin>", :row 11, :col 4, :level :warning,
        :message "Protocol method baz is implemented with arity 4 but expects 1, 2, 3"})
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
        :message "Protocol method foo is implemented with arity 2 but expects 1"}
       {:file "<stdin>", :row 11, :col 4, :level :warning,
        :message "Protocol method bar is implemented with arity 3 but expects 1, 2"})
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
        :message "Protocol method foo is implemented with arity 2 but expects 1"})
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
        :message "Protocol method bar is implemented with arity 3 but expects 1, 2"})
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
        :message "Protocol method foo is implemented with arity 2 but expects 1"})
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
        :message "Protocol method bar is implemented with arity 3 but expects 1, 2"})
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
  (bar [x] :ok)
  (bar [x y] :ok)
  (bar [x y z] :ok))

(reify P
  (foo [this] :ok)
  (bar [this] :ok)
  (bar [this x] :ok)
  (bar [this x y] :ok))

(extend-protocol P
  String
  (foo [x] :ok)
  (bar [x] :ok)
  (bar [x y] :ok)
  (bar [x y z] :ok))

(extend-type Number
  P
  (foo [x] :ok)
  (bar [x] :ok)
  (bar [x y] :ok)
  (bar [x y z] :ok))")))))

(deftest multi-body-wrong-arity-test
  (testing "multi-body method impl with wrong arity"
    (assert-submaps2
     '({:file "<stdin>", :row 9, :col 4, :level :warning,
        :message "Protocol method bar is implemented with arity 4 but expects 1, 2, 3"})
     (lint! "(ns test.foo)

(defprotocol P
  (foo [a])
  (bar [a] [a b] [a b c]))

(reify P
  (foo [this] :ok)
  (bar ([this] :ok)
       ([this x] :ok)
       ([this x y z] :wrong)))")))
  (testing "multi-body method impl all correct"
    (is (empty?
         (lint! "(ns test.foo)

(defprotocol P
  (bar [a] [a b] [a b c]))

(reify P
  (bar ([this] :ok)
       ([this x] :ok)
       ([this x y] :ok)))")))))

(deftest missing-arity-test
  (testing "warns when protocol arity is not implemented"
    (assert-submaps2
     '({:level :warning,
        :message "Protocol method bar arities 2, 3 are not implemented"})
     (lint! "(ns test.foo)

(defprotocol P
  (bar [a] [a b] [a b c]))

(deftype T []
  P
  (bar [this] :ok))"
           {:linters {:missing-protocol-method-arity {:level :warning}}})))
  (testing "no warning when all arities are implemented"
    (is (empty?
         (lint! "(ns test.foo)

(defprotocol P
  (bar [a] [a b]))

(deftype T []
  P
  (bar [this] :ok)
  (bar [this x] :ok))"
               {:linters {:missing-protocol-method-arity {:level :warning}}}))))
  (testing "no warning for entirely missing method (covered by missing-protocol-method)"
    (is (empty?
         (lint! "(ns test.foo)

(defprotocol P
  (foo [a])
  (bar [a] [a b]))

(deftype T []
  P
  (foo [this] :ok))"
               {:linters {:missing-protocol-method {:level :off}
                           :missing-protocol-method-arity {:level :warning}}}))))
  (testing "off by default"
    (is (empty?
         (lint! "(ns test.foo)

(defprotocol P
  (bar [a] [a b] [a b c]))

(deftype T []
  P
  (bar [this] :ok))")))))

(deftest definterface-wrong-arity-test
  (testing "definterface method implemented with wrong arity"
    (assert-submaps2
     '({:level :warning,
        :message "Protocol method foo is implemented with arity 1 but expects 2"})
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

(deftest definterface-overloaded-method-test
  (testing "no warning when definterface declares the same method with multiple arities (#2814)"
    (is (empty?
         (lint! "(ns test.foo)

(definterface IStore
  (put [_v])
  (put [_k _v]))

(deftype Store []
  IStore
  (put [_this _object] nil)
  (put [_this _k _object] nil))"))))
  (testing "wrong arity is still detected when definterface overloads a method"
    (assert-submaps2
     '({:level :warning,
        :message "Protocol method put is implemented with arity 4 but expects 2, 3"})
     (lint! "(ns test.foo)

(definterface IStore
  (put [_v])
  (put [_k _v]))

(deftype Store []
  IStore
  (put [_this _object] nil)
  (put [_this _k _object] nil)
  (put [_this _a _b _c] nil))"))))

(deftest config-disabled-test
  (testing "linter can be disabled via config"
    (is (empty?
         (lint! "(ns test.foo)

(defprotocol P
  (foo [a]))

(deftype T []
  P
  (foo [x y] :wrong-arity))"
               {:linters {:protocol-method-arity-mismatch {:level :off}}})))))

(deftest config-in-ns-test
  (testing "linter can be disabled via config-in-ns"
    (is (empty?
         (lint! "(ns repro)

(defprotocol P
  (foo [a]))

(deftype T []
  P
  (foo [x y] :wrong-arity))"
               '{:config-in-ns {repro {:linters {:protocol-method-arity-mismatch {:level :off}}}}})))))

(deftest ignore-test
  (testing "#_:clj-kondo/ignore on top-level form ignores wrong arity"
    (is (empty?
         (lint! "(ns test.foo)
(defprotocol P (bar [a]))
#_:clj-kondo/ignore
(deftype T [] P (bar [this x y] :wrong))"))))
  (testing "targeted ignore on the protocol method form ignores wrong arity"
    (is (empty?
         (lint! "(ns test.foo)
(defprotocol P (bar [a]))
(deftype T []
  P
  #_{:clj-kondo/ignore [:protocol-method-arity-mismatch]}
  (bar [this x y] :wrong))"))))
  (testing "#_:clj-kondo/ignore on top-level form ignores missing arity"
    (is (empty?
         (lint! "(ns test.foo)
(defprotocol P (bar [a] [a b] [a b c]))
#_:clj-kondo/ignore
(deftype T [] P (bar [this] :ok))"
                {:linters {:missing-protocol-method-arity {:level :warning}}})))))

(deftest cross-file-cache-test
  (testing "arity check works across files via cache"
    (fs/with-temp-dir [tmp {}]
      (spit (fs/file tmp "proto.clj")
            "(ns proto) (defprotocol P (foo [a]))")
      (lint! (fs/file tmp "proto.clj") "--cache" (str tmp))
      (spit (fs/file tmp "impl.clj")
            "(ns impl (:require [proto])) (deftype T [] proto/P (foo [this extra] :wrong))")
      (assert-submaps2
       '({:level :warning
          :message "Protocol method foo is implemented with arity 2 but expects 1"})
       (lint! (fs/file tmp "impl.clj") "--cache" (str tmp))))))
