(ns clj-kondo.existing-alias-test
  (:require [clj-kondo.test-utils :refer [lint!]]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]))

(deftest single-alias-test
  (is (= '({:file "corpus/existing_aliases/single_alias.clj",
            :row 4,
            :col 1,
            :level :warning,
            :message "An alias is defined for baz.qux: q"})
         (lint! (io/file "corpus/existing_aliases/single_alias.clj")
                {:linters {:existing-alias {:level :warning}}}))))

(deftest multiple-aliases-test
  (is (= '({:file "corpus/existing_aliases/multiple_aliases.clj",
            :row 5,
            :col 1,
            :level :warning,
            :message "Multiple aliases are defined for baz.qux: q, qq"})
         (lint! (io/file "corpus/existing_aliases/multiple_aliases.clj")
                {:linters {:duplicate-require {:level :off}
                           :existing-alias {:level :warning}}}))))

(deftest excluded-alias-test
  (is (= '({:file "corpus/existing_aliases/excluded_alias.clj",
            :row 5,
            :col 1,
            :level :warning,
            :message "An alias is defined for baz.qux: q"})
         (lint! (io/file "corpus/existing_aliases/excluded_alias.clj")
                '{:linters {:existing-alias {:level :warning
                                             :exclude [clojure.string]}}}))))
