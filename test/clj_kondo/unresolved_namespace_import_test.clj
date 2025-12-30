(ns clj-kondo.unresolved-namespace-import-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint! with-temp-dir]]
   [clojure.java.io :as io]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest imported-but-not-required-test
  (with-temp-dir [dir "unresolved-namespace-import"]
    (let [bar-file (io/file dir "bar.clj")
          foo-file (io/file dir "foo.clj")]
      (spit bar-file "(ns bar) (deftype Bar [])")

      (testing "imported but not required Clojure-defined class should warn"
        (spit foo-file "(ns foo (:import (bar Bar))) (Bar.)")
        (assert-submaps2
         '({:file #"foo.clj$"
            :row 1
            :col 31
            :level :warning,
            :message "Imported namespace bar but it was not required."})
         (lint! [bar-file foo-file])))

      (testing "imported and required Clojure-defined class should not warn"
        (spit foo-file "(ns foo (:require [bar]) (:import (bar Bar))) (Bar.)")
        (is (empty? (lint! [bar-file foo-file]))))

      (testing "imported but not required, but in same namespace should not warn"
        (spit bar-file "(ns bar) (deftype Bar []) (Bar.)")
        (is (empty? (lint! [bar-file]))))

      (testing "imported real Java class should not warn"
        (spit foo-file "(ns foo (:import (java.io File))) (File. \"foo\")")
        (is (empty? (lint! [foo-file]))))

      (testing "multiple usages should only warn once by default"
        (spit bar-file "(ns bar) (deftype Bar [])")
        (spit foo-file "(ns foo (:import (bar Bar))) (Bar.) (Bar.)")
        (assert-submaps2
         '({:file #"foo.clj$", :row 1, :col 31, :level :warning,
            :message "Imported namespace bar but it was not required."})
         (lint! [bar-file foo-file])))

      (testing "respects :report-duplicates true"
        (spit bar-file "(ns bar) (deftype Bar [])")
        (spit foo-file "(ns foo (:import (bar Bar))) (Bar.) (Bar.)")
        (assert-submaps2
         '({:file #"foo.clj$", :row 1, :col 31, :level :warning,
            :message "Imported namespace bar but it was not required."}
           {:file #"foo.clj$", :row 1, :col 38, :level :warning,
            :message "Imported namespace bar but it was not required."})
         (lint! [bar-file foo-file] {:linters {:unresolved-namespace {:report-duplicates true}}}))))))
