(ns clj-kondo.consistent-alias-test
  (:require [clj-kondo.test-utils :as tu :refer [lint!]]
            [clojure.test :as t :refer [deftest is testing]]
            [matcher-combinators.test]))

(def consistent
  "(ns foo (:require [clojure.string :as str])) str/join
   (ns bar (:require [clojure.string :as str])) str/join")

(def inconsistent
  "(ns foo (:require [clojure.string :as s])) s/join
   (ns bar (:require [clojure.string :as str])) str/join
   (ns baz (:require [clojure.string :as s])) s/join")

(deftest uniform-alias-test
  (is (empty? (lint! consistent
                     {:linters {:consistent-alias {:enforce true}}}))))

(deftest does-not-enforce-by-default-test
  (is (empty? (lint! inconsistent))))

(deftest enforce-test
  (is (match?
       '({:file "<stdin>",
          :row 1,
          :col 39,
          :level :warning,
          :message "Inconsistent aliases #{s str} found for clojure.string"}
         {:file "<stdin>",
          :row 2,
          :col 42,
          :level :warning,
          :message "Inconsistent aliases #{s str} found for clojure.string"}
         {:file "<stdin>",
          :row 3,
          :col 42,
          :level :warning,
          :message "Inconsistent aliases #{s str} found for clojure.string"})
       (lint! inconsistent
              {:linters {:consistent-alias {:enforce true}}}))))

(deftest enforce-with-level-off-test
  (is (empty? (lint! inconsistent
                     {:linters {:consistent-alias {:enforce true
                                                   :level :off}}}))))
