(ns clj-kondo.impl.namespace-test
  (:require
   [clj-kondo.impl.namespace :refer [analyze-ns-decl resolve-name]]
   [clj-kondo.impl.utils :refer [parse-string parse-string-all]]
   [clj-kondo.test-utils :refer [assert-submap]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest analyze-ns-test
  (assert-submap
   '{:type :ns, :name foo,
     :qualify-var {quux {:ns bar :name quux}}
     :qualify-ns {bar bar
                  baz bar}
     :clojure-excluded #{get assoc time}}
   (analyze-ns-decl
    {:lang :clj}
    (parse-string "(ns foo (:require [bar :as baz :refer [quux]])
                              (:refer-clojure :exclude [get assoc time]))")))
  (testing "namespace name with metadata is properly recognized"
    (assert-submap
     '{:type :ns, :name foo}
     (analyze-ns-decl
      {:lang :clj}
      (parse-string "(ns ^{:doc \"hello\"} foo)"))))
  (testing "string namespaces should be allowed in require"
    (assert-submap
     '{:type :ns, :name foo
       :qualify-ns {bar bar
                    baz bar}}
     (analyze-ns-decl
      {:lang :clj}
      (parse-string "(ns foo (:require [\"bar\" :as baz]))"))))
  (testing ":require with simple symbol"
    (assert-submap
     '{:type :ns, :name foo
       :qualify-ns {bar bar}}
     (analyze-ns-decl
      {:lang :clj}
      (parse-string "(ns foo (:require bar))"))))
  (testing ":require with :refer :all"
    (assert-submap
     '{:type :ns, :name foo
       :refer-alls {bar #{} baz #{baz-fn}}
       :qualify-var {renamed-fn {:ns baz, :name baz-fn}}}
     (analyze-ns-decl {:lang :clj}
                      (parse-string "(ns foo (:require [bar :refer :all]
                                       [baz :refer :all :rename {baz-fn renamed-fn}]))")))))

(deftest resolve-name-test
  (let [ns (analyze-ns-decl
            {:lang :clj}
            (parse-string "(ns foo (:require [bar :as baz :refer [quux]]))"))]
    (is (= '{:ns bar :name quux}
           (resolve-name ns 'quux))))
  (let [ns (analyze-ns-decl
            {:lang :clj}
            (parse-string "(ns foo (:require [bar :as baz :refer [quux]]))"))]
    (is (= '{:ns bar :name quux}
           (resolve-name ns 'quux))))
  (let [ns (analyze-ns-decl
            {:lang :clj}
            (parse-string "(ns clj-kondo.impl.utils {:no-doc true} (:require [rewrite-clj.parser :as p]))
"))]
    (is (= '{:ns rewrite-clj.parser :name parse-string}
           (resolve-name ns 'p/parse-string))))
  (testing "referring to unknown namespace alias"
    (let [ns (analyze-ns-decl
              {:lang :clj}
              (parse-string "(ns clj-kondo.impl.utils {:no-doc true})
"))]
      (nil? (resolve-name ns 'p/parse-string))))
  (testing "referring with full namespace"
    (let [ns (analyze-ns-decl
              {:lang :clj}
              (parse-string "(ns clj-kondo.impl.utils (:require [clojure.core]))
(clojure.core/inc 1)
"))]
      ;; TODO: what's the test here?
      (is (=
           '{:ns clojure.core :name inc}
           (resolve-name ns 'clojure.core/inc))))))

(comment
  (t/run-tests)
  (analyze-ns-decl
   {:lang :clj}
   (parse-string "(ns foo (:require [bar :as baz :refer [quux]]))"))
  )
