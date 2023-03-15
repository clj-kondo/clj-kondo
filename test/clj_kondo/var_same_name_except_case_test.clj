(ns clj-kondo.var-same-name-except-case-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps2] :rename {assert-submaps2 assert-submaps}]
   [clojure.test :refer [deftest is]]))

(deftest var-same-except-name-case-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 17, :level :error,
      :message "foo differs only in case from Foo"})
   (lint! "(defn Foo [] 1) (defn- foo [] 2) (defn use-foo [] (foo))"
          '{:linters {:var-same-name-except-case {:level :error}}}))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 21, :level :error,
      :message "Foo differs only in case from foo"})
   (lint! "(defmacro foo [] 1) (definline Foo [] 2)"
          '{:linters {:var-same-name-except-case {:level :error}}}))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 31, :level :error,
      :message "foo differs only in case from Foo"})
   (lint! "(defprotocol Foo (bar [_] 1)) (definterface foo (baz [] 2))"
          '{:linters {:var-same-name-except-case {:level :error}}}))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 19, :level :error,
      :message "FOO differs only in case from Foo"})
   (lint! "(deftype Foo [_]) (defrecord FOO [_])"
          '{:linters {:var-same-name-except-case {:level :error}}}))
  ;; avoiding false positives
  (is (empty?
       (lint! "(def leppard 1) (def Leppard 2)"
              '{:linters {:var-same-name-except-case {:level :error}}})))
  (is (empty?
       (lint! "(defprotocol Foo (foo [_] 1))"
              '{:linters {:var-same-name-except-case {:level :error}}})))
  (is (empty?
       (lint! "(definterface Foo (foo [] 1))"
              '{:linters {:var-same-name-except-case {:level :error}}})))
  (is (empty?
       (lint! "(defprotocol FOO (Foo [_] 1)) (defn foo [] 2)"
              '{:linters {:var-same-name-except-case {:level :error}}}))))
