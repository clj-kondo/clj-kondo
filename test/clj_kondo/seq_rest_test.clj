(ns clj-kondo.seq-rest-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest]]))

(def config {:linters {:seq-rest {:level :warning}}})

(deftest seq-rest-test
  (assert-submaps2
   '({:file "<stdin>"
      :row 1
      :col 1
      :level :warning
      :message "Prefer (next x) over (seq (rest x))"})
   (lint! "(seq (rest (range 42)))" config))
  (assert-submaps2
   '({:file "<stdin>"
      :row 1
      :col 15
      :level :warning
      :message "Prefer (next x) over (seq (rest x))"})
   (lint! "(-> (rest 42) seq)" config)))
