(ns clj-kondo.reader-error-test
  (:require [clj-kondo.test-utils :as tu :refer [lint!]]
            [clojure.test :as t :refer [deftest is]]))

(deftest reader-escape-char-error-test
  (is (= [{:file "<stdin>",
           :row 2,
           :col 3,
           :level :error,
           :message "Unsupported escape character: \\a."}
          {:file "<stdin>",
           :row 3,
           :col 2,
           :level :error,
           :message "Unsupported escape character: \\v."}]
         (lint! "12356
  \"\\a\" 
 {\"\\v\" \"\\V\"}"
                {:linters {:reader-error {:level :error}}}))))
