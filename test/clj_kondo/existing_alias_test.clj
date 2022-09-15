(ns clj-kondo.existing-alias-test
  (:require [clj-kondo.test-utils :refer [lint!]]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]))

(deftest single-alias-test
  (let [file (io/file "corpus" "existing_aliases" "single_alias.clj")]
    (is (= [{:file (str file),
             :row 4,
             :col 1,
             :level :warning,
             :message "An alias is defined for baz.qux: q"}]
           (lint! file {:linters {:existing-alias {:level :warning}}})))))

(deftest multiple-aliases-test
  (let [path (io/file "corpus" "existing_aliases" "multiple_aliases.clj")]
    (is (= [{:file (str path),
             :row 5,
             :col 1,
             :level :warning,
             :message "Multiple aliases are defined for baz.qux: q, qq"}]
           (lint! path
                  {:linters {:duplicate-require {:level :off}
                             :existing-alias {:level :warning}}})))))

(deftest excluded-alias-test
  (let [path (io/file "corpus" "existing_aliases" "excluded_alias.clj")]
    (is (= [{:file (str path),
             :row 5,
             :col 1,
             :level :warning,
             :message "An alias is defined for baz.qux: q"}]
           (lint! path
                  '{:linters {:existing-alias {:level :warning
                                               :exclude [clojure.string]}}})))))
