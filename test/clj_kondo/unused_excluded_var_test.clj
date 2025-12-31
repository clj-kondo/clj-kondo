(ns clj-kondo.unused-excluded-var-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is testing]]))

(deftest unused-excluded-var-test
  (testing "unused excluded var"
    (assert-submaps2
     [{:row 1
       :col 35
       :message "Unused excluded var: read"
       :level :info
       :file "<stdin>"}]
     (lint!
      "(ns foo (:refer-clojure :exclude [read]))")))

  (testing "used excluded var"
    (is (empty? (lint!
                 "(ns foo (:refer-clojure :exclude [read]))
             (defn read [])"))))

  (testing "used excluded var in binding"
    (is (empty? (lint!
                 "(ns foo (:refer-clojure :exclude [read]))
             (let [read 1] read)"))))

  (testing "multiple unused excluded vars"
    (assert-submaps2
     [{:row 1
       :col 35
       :message "Unused excluded var: read"
       :level :info
       :file "<stdin>"}
      {:row 1
       :col 40
       :message "Unused excluded var: read-string"
       :level :info
       :file "<stdin>"}]
     (lint!
      "(ns foo (:refer-clojure :exclude [read read-string]))")))

  (testing "mixed used and unused excluded vars"
    (assert-submaps2
     [{:row 1
       :col 40
       :message "Unused excluded var: read-string"
       :level :info
       :file "<stdin>"}]
     (lint!
      "(ns foo (:refer-clojure :exclude [read read-string]))
                          (defn read [])")))

  (testing "linter disabled"
    (is (empty?
         (lint!
          "(ns foo {:clj-kondo/config {:linters {:unused-excluded-var {:level :off}}}}
               (:refer-clojure :exclude [read]))")))
    (is (empty?
         (lint! "(ns foo (:refer-clojure :exclude [read read-string]))"
                {:linters {:unused-excluded-var {:level :off}}}))))

  (testing "excluded var shadowed by require"
    (is (empty? (lint! "(ns foo (:refer-clojure :exclude [comp]) (:require [other-ns :refer [comp]])) comp"))))

  (testing "excluded var not used when shadowed by require with different name"
    (assert-submaps2
     [{:row 2
       :col 31
       :message "Unused excluded var: replace"
       :level :info
       :file "<stdin>"}]
     (lint!
      "(ns foo (:require [lib.util.match])
    (:refer-clojure :exclude [replace]))
 (defn desugar-does-not-contain
     [m]
     (lib.util.match/replace m
       [:does-not-contain & args]
       [:not (into [:contains] args)]))"))))

(deftest issue-2704-test
  (testing "defmulti defines var"
    (is (empty? (lint! "(ns sci.impl.core-protocols
  {:no-doc true}
  (:refer-clojure :exclude [deref -deref])
  (:require
   [sci.impl.types :as types]))

(defmulti #?(:clj deref :cljs -deref) types/type-impl)"
                       "--filename" "sci/impl.core_protocols.cljc")))))
