(ns clj-kondo.ignore-test
  (:require [clj-kondo.test-utils :refer [lint!]]
            [clojure.test :as t :refer [deftest is testing]]))

(deftest ignore-test
  (is (empty? (lint! "#_:clj-kondo/ignore (inc :foo)"
                     {:linters {:type-mismatch {:level :warning}}})))
  (is (empty? (lint! "#_:clj-kondo/ignore (inc 1 2 3)"
                     {:linters {:type-mismatch {:level :warning}}})))
  (is (empty? (lint! "#_:clj-kondo/ignore (defn foo [] (inc 1 2 3))"
                     {:linters {:type-mismatch {:level :warning}}})))
  (is (empty? (lint! "#_:clj-kondo/ignore (defn foo [] x)"
                     {:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "#_:clj-kondo/ignore (\"foo\")"
                     {:linters {:not-a-function {:level :error}}})))
  (is (empty? (lint! "#_:clj-kondo/ignore {:A}"
                     {:syntax {:level :error}}))))

;; TODO: (let [#_:clj-kondo/ignore x 1])



