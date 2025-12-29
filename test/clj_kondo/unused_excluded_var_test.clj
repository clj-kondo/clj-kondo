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
       :level :warning
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
       :level :warning
       :file "<stdin>"}
      {:row 1
       :col 40
       :message "Unused excluded var: read-string"
       :level :warning
       :file "<stdin>"}]
     (lint!
      "(ns foo (:refer-clojure :exclude [read read-string]))")))

  (testing "mixed used and unused excluded vars"
    (assert-submaps2
     [{:row 1
       :col 40
       :message "Unused excluded var: read-string"
       :level :warning
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
  
  (testing "excluded var used via conditional import and macro definition"
    (is (empty? (lint! "(ns foo
      (:refer-clojure :exclude [when-some])
    (:require clojure.walk
              clojure.set
              #?(:cljs [cljs.core :as core])))
    #?(:clj
       (import-macros clojure.core
                      [when-some]))
    
    #?(:cljs
       (core/defmacro when-some
         \"bindings => binding-form test
    
          When test is not nil, evaluates body with binding-form bound to the
          value of test\"
         [bindings & body]
         (assert-args when-some
                      (vector? bindings) \"a vector for its binding\"
                      (= 2 (count bindings)) \"exactly 2 forms in binding vector\")
         (core/let [form (bindings 0) tst (bindings 1)]
           `(let [temp# ~tst]
              (if (nil? temp#)
                nil
                (let [~form temp#]
                  ~@body))))))"
                       "--lang" "cljc"))))

  (testing "excluded var not used when shadowed by require with different name"
    (assert-submaps2
     [{:row 2
       :col 31
       :message "Unused excluded var: replace"
       :level :warning
       :file "<stdin>"}]
     (lint!
      "(ns foo (:require [lib.util.match])
    (:refer-clojure :exclude [replace]))
             
   (defn desugar-does-not-contain
     [m]
     (lib.util.match/replace m
       [:does-not-contain & args]
       [:not (into [:contains] args)]))"))))
 