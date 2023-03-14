(ns clj-kondo.var-same-except-case-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps2] :rename {assert-submaps2 assert-submaps}]
   [clojure.test :refer [deftest is]]))

(deftest var-same-except-case-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 17, :level :error,
      :message "Foo differs only in case from foo"})
   (lint! "(defn foo [] 1) (defn Foo [] 2)"
          '{:linters {:var-same-except-case {:level :error}}}))
  ;; avoiding false positives
  (is (empty?
       (lint! "(def foo 1) (def Foo 2)"
              '{:linters {:var-same-except-case {:level :error}}}))))
