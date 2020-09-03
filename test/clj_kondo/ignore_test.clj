(ns clj-kondo.ignore-test
  (:require [clj-kondo.test-utils :refer [lint!]]
            [clojure.test :as t :refer [deftest is testing]]))

(deftest ignore-test
  (is (empty? (lint! "#_:clj-kondo/ignore (inc :foo)"
                     {:linters {:type-mismatch {:level :warning}}})))
  (is (empty? (lint! "#_:clj-kondo/ignore (inc 1 2 3)"
                     {:linters {:type-mismatch {:level :warning}}})))
  (is (empty? (lint! "#_:clj-kondo/ignore (defn foo [] (inc 1 2 3))"
                     {:linters {:type-mismatch {:level :warning}}}))))

;; TODO: #_:clj-kondo/ignore x

