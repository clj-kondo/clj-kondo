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
                {:linters {:unused-excluded-var {:level :off}}})))))
