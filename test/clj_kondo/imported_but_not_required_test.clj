(ns clj-kondo.imported-but-not-required-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint! with-temp-dir]]
   [clojure.java.io :as io]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest imported-but-not-required-test
  (with-temp-dir [dir "imported-but-not-required"]
    (let [bar-file (io/file dir "bar.clj")
          foo-file (io/file dir "foo.clj")]
      (spit bar-file "(ns bar) (deftype Bar [])")

      (testing "imported but not required Clojure-defined class should warn"
        (spit foo-file "(ns foo (:import (bar Bar))) (Bar.)")
        (assert-submaps2
         '({:file #"foo.clj$", :row 1, :col 23, :level :warning,
            :message "Imported namespace bar but it was not required."})
         (lint! [bar-file foo-file])))

      (testing "imported and required Clojure-defined class should not warn"
        (spit foo-file "(ns foo (:require [bar]) (:import (bar Bar))) (Bar.)")
        (is (empty? (lint! [bar-file foo-file]))))

      (testing "hyphenated namespace imported via munged package name"
        (spit bar-file "(ns bar-baz) (deftype Bar [])")
        (spit foo-file "(ns foo (:require [bar-baz]) (:import (bar_baz Bar))) (Bar.)")
        (is (empty? (lint! [bar-file foo-file]))))

      (testing "hyphenated namespace imported via munged package name but not required"
        (spit bar-file "(ns bar-baz) (deftype Bar [])")
        (spit foo-file "(ns foo (:import (bar_baz Bar))) (Bar.)")
        (assert-submaps2
         '({:file #"foo.clj$", :row 1, :col 27, :level :warning,
            :message "Imported namespace bar-baz but it was not required."})
         (lint! [bar-file foo-file])))

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
         '({:file #"foo.clj$", :row 1, :col 23, :level :warning,
            :message "Imported namespace bar but it was not required."})
         (lint! [bar-file foo-file])))

      (testing "respects :report-duplicates true"
        (spit bar-file "(ns bar) (deftype Bar [])")
        (spit foo-file "(ns foo (:import (bar Bar))) (Bar.) (Bar.)")
        (assert-submaps2
         '({:file #"foo.clj$", :row 1, :col 23, :level :warning,
            :message "Imported namespace bar but it was not required."})
         (lint! [bar-file foo-file] {:linters {:imported-but-not-required {:report-duplicates true}}})))

      (testing "disabling linter via config"
        (spit bar-file "(ns bar) (deftype Bar [])")
        (spit foo-file "(ns foo (:import (bar Bar))) (Bar.)")
        (is (empty? (lint! [bar-file foo-file] {:linters {:imported-but-not-required {:level :off}}}))))

      (testing "disabling linter via inline ignore"
        (spit bar-file "(ns bar) (deftype Bar [])")
        (spit foo-file "(ns ^{:clj-kondo/ignore [:imported-but-not-required]} foo (:import (bar Bar))) (Bar.)")
        (is (empty? (lint! [bar-file foo-file]))))

      (testing "disabling linter via config-in-ns"
        (spit bar-file "(ns bar) (deftype Bar [])")
        (spit foo-file "(ns ^{:clj-kondo/config {:linters {:imported-but-not-required {:level :off}}}} foo (:import (bar Bar))) (Bar.)")
        (is (empty? (lint! [bar-file foo-file]))))

      (testing "fully-qualified class used without import or require should warn"
        (spit bar-file "(ns bar) (deftype Bar [])")
        (spit foo-file "(ns foo) (bar.Bar.)")
        (assert-submaps2
         '({:file #"foo.clj$", :row 1, :level :warning,
            :message "Used Clojure namespace bar but it was not required."})
         (lint! [bar-file foo-file])))

      (testing "fully-qualified class used with require but no import should not warn"
        (spit bar-file "(ns bar) (deftype Bar [])")
        (spit foo-file "(ns foo (:require [bar])) (bar.Bar.)")
        (is (empty? (lint! [bar-file foo-file]))))

      (testing "fully-qualified class using new form without require should warn"
        (spit bar-file "(ns bar) (deftype Bar [])")
        (spit foo-file "(ns foo) (new bar.Bar)")
        (assert-submaps2
         '({:file #"foo.clj$", :row 1, :level :warning,
            :message "Used Clojure namespace bar but it was not required."})
         (lint! [bar-file foo-file])))

      (testing "fully-qualified class in same namespace should not warn"
        (spit bar-file "(ns bar) (deftype Bar []) (bar.Bar.)")
        (is (empty? (lint! [bar-file]))))

      (testing "fully-qualified real Java class should not warn"
        (spit foo-file "(ns foo) (java.io.File. \"foo\")")
        (is (empty? (lint! [foo-file]))))

      (testing "hyphenated namespace used by fully-qualified class without require should warn"
        (spit bar-file "(ns bar-baz) (deftype Bar [])")
        (spit foo-file "(ns foo) (bar_baz.Bar.)")
        (assert-submaps2
         '({:file #"foo.clj$", :row 1, :level :warning,
            :message "Used Clojure namespace bar-baz but it was not required."})
         (lint! [bar-file foo-file]))))))