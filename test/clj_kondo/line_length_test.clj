(ns clj-kondo.line-length-test
  (:require
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.test-utils :refer [assert-submaps assert-submaps2 lint!]]
   [clojure.string :as string]
   [clojure.test :refer [deftest is testing]]))

(def short-line "(+ 1 2 3)")
(def short-lines "(+ 1 2 3)\n(+ 42 3.14159265358979 'TAU)\n")

(def long-line
  (str "(println \""
       "This is a very long string that could be tricky to read in an editor if you didn't have access to good"
       " automatic line wrapping, or perhaps found that automatic line wrapping made the code structure difficult"
       " to parse in your head. The point is that it is both more than 80 and more than 120 characters long."
       "\")"))

(def multi-line (string/join "\n" [short-line short-line long-line short-line long-line short-line]))

(deftest line-is-too-long-test

  (testing "test linting short lines"
    (is (empty? (lint! short-line)))
    (is (empty? (lint! short-line "--lang" "cljs")))
    (is (empty? (lint! short-line '{:linters {:line-length {:max-line-length 80
                                                            :level :warning}}})))
    (is (empty? (lint! short-line '{:linters {:line-length {:max-line-length 80
                                                            :level :warning}}} "--lang" "cljs")))
    (is (empty? (lint! short-lines '{:linters {:line-length {:max-line-length 80
                                                             :level :warning}}})))
    (is (empty? (lint! short-lines '{:linters {:line-length {:max-line-length 80
                                                             :level :warning}}} "--lang" "cljs")))
    (assert-submaps '({:file "<stdin>"
                       :level :warning
                       :message "Line is longer than 2 characters."
                       :row 1
                       :col 3}) (lint! short-line '{:linters {:line-length {:max-line-length 2
                                                                            :level :warning}}}))
    (assert-submaps '({:file "<stdin>"
                       :level :warning
                       :message "Line is longer than 2 characters."
                       :row 1
                       :col 3}) (lint! short-line '{:linters {:line-length {:max-line-length 2
                                                                            :level :warning}}}
                                       "--lang" "cljs")))

  (testing "test linting long lines"
    (is (empty? (lint! multi-line '{:linters {:line-length {:max-line-length 8000
                                                            :level :warning}}})))
    (assert-submaps
     '({:file "<stdin>"
        :level :warning
        :message "Line is longer than 80 characters."
        :row 1
        :col 81})
     (lint! long-line '{:linters {:line-length {:max-line-length 80
                                                :level :warning}}}))
    (assert-submaps
     '({:file "<stdin>"
        :level :warning
        :message "Line is longer than 120 characters."
        :row 3
        :col 121}
       {:file "<stdin>"
        :level :warning
        :message "Line is longer than 120 characters."
        :row 5
        :col 121})
     (lint! multi-line '{:linters {:line-length {:max-line-length 120
                                                 :level :warning}}}))))

(deftest exclusions-test
  (is (empty? (lint! " ;; https://clojurians-log.clojureverse.org/clojure-spec/2017-08-12/1502573905.650871"
                     '{:linters {:line-length {:exclude-urls true
                                               :level :warning
                                               :max-line-length 80}}})))
  (is (empty? (lint! " ;; :ll/ok"
                     '{:linters {:line-length {:exclude-pattern ";; :ll/ok"
                                               :level :warning
                                               :max-line-length 1}}}))))

(deftest end-col-end-row-test
  (let [res (with-in-str "(ns repro)
(def this-is-a-very-long-symbol-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa nil)
;; This is a very long comment aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
              (clj-kondo/run! {:lint ["-"]
                               :config {:linters {:line-length {:level :warning
                                                                :max-line-length 80}}}}))
        findings (:findings res)]
    (assert-submaps2 [{:message "Line is longer than 80 characters.",
                       :type :line-length,
                       :row 2,
                       :end-row 2,
                       :col 81,
                       :end-col 92,
                       :level :warning}
                      {:message "Line is longer than 80 characters.",
                       :type :line-length,
                       :row 3,
                       :end-row 3,
                       :col 81,
                       :end-col 92,
                       :level :warning}] findings)))
