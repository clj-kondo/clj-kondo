(ns clj-kondo.clj-kondo-config-test
  (:require
   [clj-kondo.core :as core]
   [clj-kondo.impl.version :as version]
   [clj-kondo.test-utils :refer [lint! assert-submaps assert-submaps2]]
   [clojure.string :as str]
   [clojure.test :refer [deftest testing is]])
  (:import
   java.io.StringWriter
   java.time.format.DateTimeFormatter
   java.time.LocalDate))

(deftest unexpected-linter-name-test
  (testing "Unexpected linter name"
    (assert-submaps
     '({:file ".clj-kondo/config.edn", :row 1, :col 12, :level :warning, :message "Unexpected linter name: :foo"})
     (lint! "{:linters {:foo 1}}" "--filename" ".clj-kondo/config.edn")))
  (testing "Linter config should go under :linters"
    (assert-submaps
     '({:file ".clj-kondo/config.edn", :row 1, :col 2, :level :warning, :message "Linter config should go under :linters"})
     (lint! "{:unresolved-symbol {}}" "--filename" ".clj-kondo/config.edn"))))

(deftest should-be-map-test
  (testing "Top level maps"
    (assert-submaps
     '({:file ".clj-kondo/config.edn", :row 1, :col 11, :level :warning, :message "Expected a map, but got: int"})
     (lint! "{:linters 1}" "--filename" ".clj-kondo/config.edn")))
  (testing "Linter config"
    (assert-submaps
     '({:file ".clj-kondo/config.edn", :row 1, :col 28, :level :warning, :message "Expected a map, but got: int"})
     (lint! "{:linters {:unused-binding 1}}" "--filename" ".clj-kondo/config.edn"))))

(deftest qualified-symbol-test
  (testing "Top level maps"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 1, :level :error, :message "Unresolved symbol: x"})
     (lint! "x" '{:linters {:unresolved-symbol {:exclude [(foo.bar)]
                                                :level :error}}}))))

(defn ^:private version-shifted-by-days
  "Extracts the date part of the version, adds to it
   the given number of days and returns the result
   as a version string"
  [days]
  (let [date-part (first (str/split
                          version/version
                          #"\-"))
        ^LocalDate date (LocalDate/parse
                         date-part
                         (DateTimeFormatter/ofPattern
                          "yyyy.MM.dd"))
        shifted (.plusDays date days)]
    (.format
     shifted
     (DateTimeFormatter/ofPattern
      "yyyy.MM.dd"))))

(defn- run-and-capture-err-out
  "Runs the linter on file content with given configuration
   assuming the file has given name, captures and returns
   anything that would be printed to *err*"
  [content config filename]
  (let [sw (StringWriter.)]
    (core/run! {:lint [content]
                :config config
                :filename filename
                :err-out sw
                :cache false})
    (str sw)))

(deftest minimum-version-test
  (testing "No finding when version equal to minimum"
    (let [output (run-and-capture-err-out
                  ""
                  {:min-clj-kondo-version version/version}
                  ".clj-kondo/config.edn")]
      (is (empty? (str/replace output "\n" "")))))
  (testing "No finding when version after minimum"
    (let [output (run-and-capture-err-out
                  ""
                  {:min-clj-kondo-version (version-shifted-by-days -1)}
                  ".clj-kondo/config.edn")]
      (is (empty? (str/replace output "\n" "")))))
  (testing "Find when version before minimum"
    (let [output (run-and-capture-err-out
                  ""
                  {:min-clj-kondo-version (version-shifted-by-days 1)}
                  ".clj-kondo/config.edn")]
      (is
       (str/includes?
        output
        "Version"))
      (is
       (str/includes?
        output
        version/version))
      (is
       (str/includes?
        output
        "below configured minimum"))
      (is
       (str/includes?
        output
        (version-shifted-by-days 1))))))
