(ns clj-kondo.unsupported-escape-character-test
  (:require [clj-kondo.test-utils :as tu :refer [lint! assert-submaps2]]
            [clojure.test :as t :refer [deftest is]]))

(deftest reader-escape-char-error-test
  (assert-submaps2 [{:file "<stdin>",
                    :row 2,
                    :col 3,
                    :level :error,
                    :message "Unsupported escape character: \\a."}
                   {:file "<stdin>",
                    :row 3,
                    :col 3,
                    :level :error,
                    :message "Unsupported escape character: \\v."}
                    {:file "<stdin>",
                     :row 3,
                     :col 8,
                     :level :error,
                     :message "Unsupported escape character: \\V."}]
                  (lint! "12356
  \"\\a\" 
 {\"\\v\" \"\\V\"}"
                         {:linters {:reader-error {:level :error}}})))
