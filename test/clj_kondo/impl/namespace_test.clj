(ns clj-kondo.impl.namespace-test
  (:require
   [clj-kondo.impl.namespace :refer [analyze-ns-decl]]
   [clj-kondo.impl.utils :refer [parse-string parse-string-all]]
   [clj-kondo.test-utils :refer [submap?]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest analyze-ns-test
  (is
   (submap?
    '{:type :ns, :name foo,
      :qualify-var {quux {:ns bar :name quux}}
      :qualify-ns {bar bar
                   baz bar}
      :clojure-excluded #{get assoc time}}
    (analyze-ns-decl
     :clj
     (parse-string "(ns foo (:require [bar :as baz :refer [quux]])
                              (:refer-clojure :exclude [get assoc time]))"))))
  (testing "string namespaces should be allowed in require"
    (is (submap?
         '{:type :ns, :name foo
           :qualify-ns {bar bar
                        baz bar}}
         (analyze-ns-decl
          :clj
          (parse-string "(ns foo (:require [\"bar\" :as baz]))")))))
  (testing ":require with simple symbol"
    (is (submap?
         '{:type :ns, :name foo
           :qualify-ns {bar bar}}
         (analyze-ns-decl
          :clj
          (parse-string "(ns foo (:require bar))")))))
  (testing ":require with :refer :all"
    (is (submap?
         '{:type :ns, :name foo
           :refer-alls {bar #{} baz #{}}
           :qualify-var {renamed-fn {:ns baz, :name baz-fn}}}
         (analyze-ns-decl :clj
                          (parse-string-all "(ns foo (:require [bar :refer :all]
                                               [baz :refer :all :rename {baz-fn renamed-fn}]))"))))))

(comment
  (t/run-tests)
  (analyze-ns-decl
   :clj
   (parse-string "(ns foo (:require [bar :as baz :refer [quux]]))"))
  )
