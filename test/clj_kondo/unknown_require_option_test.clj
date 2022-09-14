(ns clj-kondo.unknown-require-option-test
  (:require [clj-kondo.test-utils :refer [lint!]]
            [clojure.test :refer [deftest is]]))

(deftest unknown-require-option-test
  (is (= '({:file "<stdin>",
            :row 1,
            :col 24,
            :level :warning,
            :message "Unknown :require option: :s"})
         (lint! "(ns foo (:require [bar :s b]))"
                {:linters {:unknown-require-option {:level :warning}}}))))

(deftest ignorable-test
  (is (= '()
         (lint! "(ns foo (:require #_:clj-kondo/ignore [bar :s b]))"
                {:linters {:unknown-require-option {:level :warning}}}))))
