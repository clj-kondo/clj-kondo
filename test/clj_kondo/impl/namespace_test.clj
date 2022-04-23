(ns clj-kondo.impl.namespace-test
  (:require
   [clj-kondo.impl.analyzer.namespace :refer [analyze-ns-decl]]
   [clj-kondo.impl.namespace :refer [resolve-name]]
   [clj-kondo.impl.utils :refer [parse-string]]
   [clj-kondo.test-utils :refer [assert-submap]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest analyze-ns-test
  (assert-submap
   '{:type :ns, :name foo,
     :referred-vars {quux {:ns bar :name quux}}
     :qualify-ns {bar bar
                  baz bar}
     :aliases {baz bar}
     :clojure-excluded #{get assoc time}}
   (analyze-ns-decl
    {:lang :clj
     :namespaces (atom {})
     :used-namespaces (atom {})}
    (parse-string "(ns foo (:require [bar :as baz :refer [quux]])
                              (:refer-clojure :exclude [get assoc time]))")))
  (testing "namespace name with metadata is properly recognized"
    (assert-submap
     '{:type :ns, :name foo}
     (analyze-ns-decl
      {:lang :clj
       :namespaces (atom {})
       :used-namespaces (atom {})}
      (parse-string "(ns ^{:doc \"hello\"} foo)"))))
  (testing "string namespaces should be allowed in require"
    (assert-submap
     '{:type :ns, :name foo
       :qualify-ns {bar bar
                    baz bar}
       :aliases {baz bar}}
     (analyze-ns-decl
      {:lang :clj
       :namespaces (atom {})
       :used-namespaces (atom {})}
      (parse-string "(ns foo (:require [\"bar\" :as baz]))"))))
  (testing ":require with simple symbol"
    (assert-submap
     '{:type :ns, :name foo
       :qualify-ns {bar bar}}
     (analyze-ns-decl
      {:lang :clj
       :namespaces (atom {})
       :used-namespaces (atom {})}
      (parse-string "(ns foo (:require bar))"))))
  (testing ":require with :refer :all"
    (assert-submap
     '{:type :ns, :name foo
       :refer-alls {bar {:excluded #{}} baz {:excluded #{baz-fn}}}
       :referred-vars {renamed-fn {:ns baz, :name baz-fn}}}
     (analyze-ns-decl {:lang :clj
                       :namespaces (atom {})
                       :findings (atom [])
                       :used-namespaces (atom {})}
                      (parse-string "(ns foo (:require [bar :refer :all]
                                       [baz :refer :all :rename {baz-fn renamed-fn}]))")))))

(deftest resolve-name-test
  (let [ctx {:used-namespaces (atom {})
             :namespaces (atom {})
             :findings (atom [])
             :base-lang :clj
             :lang :clj}
        _ (analyze-ns-decl
           ctx
           (parse-string "(ns foo (:require [bar :as baz :refer [quux]]))"))]
    (assert-submap
     '{:ns bar :name quux}
     (resolve-name ctx false 'foo 'quux nil))
    (let [_ (analyze-ns-decl
             ctx
             (parse-string "(ns foo (:require [bar :as baz :refer [quux]]))"))]
      (assert-submap '{:ns bar :name quux}
                     (resolve-name ctx false 'foo 'quux nil)))
    (let [_ (analyze-ns-decl
             ctx
             (parse-string "(ns clj-kondo.impl.utils {:no-doc true} (:require [rewrite-clj.parser :as p]))
"))]
      (is (= '{:ns rewrite-clj.parser :name parse-string :alias p}
             (resolve-name ctx false 'clj-kondo.impl.utils 'p/parse-string nil))))
    (testing "referring to unknown namespace alias"
      (let [ctx {:namespaces (atom {})
                 :findings (atom [])
                 :base-lang :clj
                 :lang :clj}
            _ (analyze-ns-decl
               ctx
               (parse-string "(ns clj-kondo.impl.utils {:no-doc true})
"))]
        (is (= 'p (:unresolved-ns (resolve-name ctx false 'clj-kondo.impl.utils 'p/parse-string nil))))))
    (testing "referring with full namespace"
      (let [_ (analyze-ns-decl
                ctx
                (parse-string "(ns clj-kondo.impl.utils (:require [clojure.core]))
(clojure.core/inc 1)
"))]
        (is (=
             '{:ns clojure.core :name inc :resolved-core? true}
             (resolve-name ctx false 'clj-kondo.impl.utils 'clojure.core/inc nil)))))))

(comment
  (t/run-tests)
  (analyze-ns-decl
   {:lang :clj}
   (parse-string "(ns foo (:require [bar :as baz :refer [quux]]))"))
  )
