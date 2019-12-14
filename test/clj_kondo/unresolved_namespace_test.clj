(ns clj-kondo.unresolved-namespace-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.java.io :as io]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest unresolved-namespace-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 2, :level :warning,
      :message "Unresolved namespace clojure.string. Are you missing a require?"})
   (lint! "(clojure.string/includes? \"foo\" \"o\")"))
  ;; avoiding false positives
  (is (empty? (lint! (io/file "project.clj"))))
  (is (empty? (lint! "js/foo" "--lang" "cljs")))
  (is (empty? (lint! "goog/foo" "--lang" "cljs")))
  (is (empty? (lint! "(java.lang.Foo/Bar)")))
  (is (empty? (lint! "(clojure.core/inc 1)"))))
