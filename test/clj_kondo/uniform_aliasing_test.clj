(ns clj-kondo.uniform-aliasing-test
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

(deftest disabled-by-default-test
  (is (empty? (lint! inconsistent))))

(deftest homogeneous-test
  (is (empty? (lint! consistent
                     {:linters {:uniform-aliasing {:level :info}}}))))

(deftest heterogeneous-test
  (is (match?
       [{:file "<stdin>",
         :row 1,
         :col 39,
         :level :info,
         :message "Different aliases #{s str} found for clojure.string"}
        {:file "<stdin>",
         :row 2,
         :col 42,
         :level :info,
         :message "Different aliases #{s str} found for clojure.string"}
        {:file "<stdin>",
         :row 3,
         :col 42,
         :level :info,
         :message "Different aliases #{s str} found for clojure.string"}]
       (lint! inconsistent
              {:linters {:uniform-aliasing {:level :info}}}))))

