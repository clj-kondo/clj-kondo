(ns clj-kondo.unsupported-escape-character-test
  (:require
   [clj-kondo.test-utils :as tu :refer [assert-submaps2 lint!]]
   [clojure.java.io :as io]
   [clojure.test :as t :refer [deftest is]]))

(deftest reader-escape-char-error-test
  (assert-submaps2 [{:file "corpus/unsupported_escape_chars.clj", :row 2, :col 1, :level :error, :message "Unsupported escape character: \\a."}
                    {:file "corpus/unsupported_escape_chars.clj", :row 3, :col 2, :level :error, :message "Unsupported escape character: \\v."}
                    {:file "corpus/unsupported_escape_chars.clj", :row 3, :col 7, :level :error, :message "Unsupported escape character: \\V."}]
                   (lint! (io/file "corpus/unsupported_escape_chars.clj")
                          {:linters {:reader-error {:level :error}}})))
