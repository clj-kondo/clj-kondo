(ns clj-kondo.impl.core-test
  (:require [clj-kondo.impl.core :as core]
            [clojure.test :as t :refer [deftest is]]))

(deftest assoc-analysis-test
  (is (= (core/assoc-analysis {:summary {}}
                              {:namespace-definitions [{:filename "/foo/bar.clj"}
                                                       {:filename "/bar/baz.clj"}]})
         {:summary {:files 2}
          :analysis {:namespace-definitions [{:filename "/foo/bar.clj"}
                                             {:filename "/bar/baz.clj"}]}})))

;;;; Scratch

(comment
  (t/run-tests)
  )
