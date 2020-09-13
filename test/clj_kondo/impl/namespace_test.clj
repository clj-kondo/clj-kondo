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
     :clojure-excluded #{get assoc time}}
   (analyze-ns-decl
    {:lang :clj
     :namespaces (atom {})}
    (parse-string "(ns foo (:require [bar :as baz :refer [quux]])
                              (:refer-clojure :exclude [get assoc time]))")))
  (testing "namespace name with metadata is properly recognized"
    (assert-submap
     '{:type :ns, :name foo}
     (analyze-ns-decl
      {:lang :clj
       :namespaces (atom {})}
      (parse-string "(ns ^{:doc \"hello\"} foo)"))))
  (testing "string namespaces should be allowed in require"
    (assert-submap
     '{:type :ns, :name foo
       :qualify-ns {bar bar
                    baz bar}}
     (analyze-ns-decl
      {:lang :clj
       :namespaces (atom {})}
      (parse-string "(ns foo (:require [\"bar\" :as baz]))"))))
  (testing ":require with simple symbol"
    (assert-submap
     '{:type :ns, :name foo
       :qualify-ns {bar bar}}
     (analyze-ns-decl
      {:lang :clj
       :namespaces (atom {})}
      (parse-string "(ns foo (:require bar))"))))
  (testing ":require with :refer :all"
    (assert-submap
     '{:type :ns, :name foo
       :refer-alls {bar {:excluded #{}} baz {:excluded #{baz-fn}}}
       :referred-vars {renamed-fn {:ns baz, :name baz-fn}}}
     (analyze-ns-decl {:lang :clj
                       :namespaces (atom {})
                       :findings (atom [])}
                      (parse-string "(ns foo (:require [bar :refer :all]
                                       [baz :refer :all :rename {baz-fn renamed-fn}]))")))))

(deftest resolve-name-test
  (let [new-ctx (fn [] {:namespaces (atom {})
                        :findings   (atom [])
                        :base-lang  :clj
                        :lang       :clj})
        ctx (new-ctx)
        _ (analyze-ns-decl
           ctx
           (parse-string "(ns foo (:require [bar :as baz :refer [quux]]))"))]
    (is (= '{:ns bar :name quux}
           (resolve-name ctx 'foo 'quux)))
    (let [_ (analyze-ns-decl
             ctx
             (parse-string "(ns foo (:require [bar :as baz :refer [quux]]))"))]
      (is (= '{:ns bar :name quux}
             (resolve-name ctx 'foo 'quux))))
    (let [_ (analyze-ns-decl
             ctx
             (parse-string "(ns clj-kondo.impl.utils {:no-doc true} (:require [rewrite-clj.parser :as p]))
"))]
      (is (= '{:ns rewrite-clj.parser :name parse-string}
             (resolve-name ctx 'clj-kondo.impl.utils 'p/parse-string))))
    (testing "referring to unknown namespace alias"
      (let [ctx {:namespaces (atom {})
                 :findings (atom [])
                 :base-lang :clj
                 :lang :clj}
            _ (analyze-ns-decl
               ctx
               (parse-string "(ns clj-kondo.impl.utils {:no-doc true})
"))]
        (is (= 'p (:unresolved-ns (resolve-name ctx 'clj-kondo.impl.utils 'p/parse-string))))))
    (testing "referring with full namespace"
      (let [_ (analyze-ns-decl
                ctx
                (parse-string "(ns clj-kondo.impl.utils (:require [clojure.core]))
(clojure.core/inc 1)
"))]
        (is (=
             '{:ns clojure.core :name inc}
             (resolve-name ctx 'clj-kondo.impl.utils 'clojure.core/inc)))))
    (testing "referring all"
      (let [code (str "(ns clj-kondo.impl.utils "
                      "  (:require [clojure.test :refer :all])) "
                      "(deftest my-test)")]
        (analyze-ns-decl ctx (parse-string code))
        (is (= '{:ns clojure.test
                 :name deftest
                 :unresolved? true
                 :clojure-excluded? false}
               (resolve-name ctx 'clj-kondo.impl.utils 'deftest)))))
    (testing "referring all for more than one namespace"
      (let [code (str "(ns clj-kondo.impl.utils "
                      "  (:require [clojure.test :refer :all]"
                      "            [clojure.string :refer :all])) "
                      "(deftest my-test (is (blank? \"\")))")]
        (let [ctx (new-ctx)]
          (analyze-ns-decl ctx (parse-string code))
          (is (= '{:ns clojure.test
                   :name deftest
                   :unresolved? true
                   :clojure-excluded? false}
                 (resolve-name ctx 'clj-kondo.impl.utils 'deftest))))
        (let [ctx (new-ctx)]
          (analyze-ns-decl ctx (parse-string code))
          ;; A known deficiency; unless specifically declared in
          ;; `clj-kondo.impl.namespace/resolve-referred-all-ns`,
          ;; any un-namespaced symbols will be assigned the first
          ;; refer-all in the requires list. In this case, `blank?`
          ;; belongs to `clojure.string`, but in this context, there
          ;; is no way to know that.
          (is (= '{:ns clojure.test
                   :name blank?
                   :unresolved? true
                   :clojure-excluded? false}
                 (resolve-name ctx 'clj-kondo.impl.utils 'blank?)))))
      (let [code (str "(ns clj-kondo.impl.utils "
                      "  (:require [clojure.string :refer :all]"
                      "            [clojure.test :refer :all])) "
                      "(deftest my-test (is (blank? \"\")))")]
        (let [ctx (new-ctx)]
          (analyze-ns-decl ctx (parse-string code))
          (is (= '{:ns                clojure.test
                   :name              deftest
                   :unresolved?       true
                   :clojure-excluded? false}
                 (resolve-name ctx 'clj-kondo.impl.utils 'deftest))))
        (let [ctx (new-ctx)]
          (analyze-ns-decl ctx (parse-string code))
          (is (= '{:ns clojure.string
                   :name blank?
                   :unresolved? true
                   :clojure-excluded? false}
                 (resolve-name ctx 'clj-kondo.impl.utils 'blank?))))))))

(comment
  (t/run-tests)
  (analyze-ns-decl
   {:lang :clj}
   (parse-string "(ns foo (:require [bar :as baz :refer [quux]]))"))
  )
