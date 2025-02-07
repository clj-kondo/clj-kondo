(ns clj-kondo.clj-kondo-config-test
  (:require
   [clj-kondo.impl.version :as version]
   [clj-kondo.test-utils :refer [lint! assert-submaps assert-submaps2 native?]]
   [clojure.string :as str]
   [clojure.test :refer [deftest testing is]])
  (:import
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
     (lint! "{:unresolved-symbol {}}" "--filename" ".clj-kondo/config.edn")))
  (testing ":min-clj-kondo-version is an expected top-level key"
    (is (empty? (lint! "{:min-clj-kondo-version \"2025.01.16\"}" "--filename" ".clj-kondo/config.edn")))))

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

(when-not native?
  (deftest minimum-version-test
    (testing "No finding when version equal to minimum"
      (is (empty? (lint!
                   ""
                   {:min-clj-kondo-version version/version}
                   "--filename"
                   ".clj-kondo/config.edn"))))
    (testing "No finding when version after minimum"
      (is (empty? (lint!
                   ""
                   {:min-clj-kondo-version (version-shifted-by-days -1)}
                   "--filename"
                   ".clj-kondo/config.edn"))))
    (testing "Find when version before minimum"
      (testing "No min-clj-kondo-version in analyzed config"
        (assert-submaps2
         [{:file "<clj-kondo>", :row 1, :col 1, :level :warning, :message
           (format "Version %s below configured minimum %s"
                   version/version
                   (version-shifted-by-days 1))}]
         (lint!
               ""
               {:min-clj-kondo-version (version-shifted-by-days 1)}
               "--filename"
               ".clj-kondo/config.edn")))

      (testing "min-clj-kondo-version in analyzed config is not the highest"
        (assert-submaps2
         [{:file "<clj-kondo>", :row 1, :col 1, :level :warning, :message
           (format "Version %s below configured minimum %s"
                   version/version
                   (version-shifted-by-days 2))}]
         (lint!
          (pr-str {:min-clj-kondo-version (version-shifted-by-days 1)})
          {:min-clj-kondo-version (version-shifted-by-days 2)}
          "--filename"
          ".clj-kondo/config.edn")))

      (testing "min-clj-kondo-version in analyzed config is the source of the finding"
        (assert-submaps2
         [{:file ".clj-kondo/config.edn", :row 1, :col 2, :level :warning, :message
           (format "Version %s below configured minimum %s"
                   version/version
                   (version-shifted-by-days 1))}]
         (lint!
          (pr-str {:min-clj-kondo-version (version-shifted-by-days 1)})
          {:min-clj-kondo-version (version-shifted-by-days 1)}
          "--filename"
          ".clj-kondo/config.edn"))))))
