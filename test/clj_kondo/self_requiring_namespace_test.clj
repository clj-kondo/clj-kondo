(ns clj-kondo.self-requiring-namespace-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps2]]
   [clojure.test :refer [deftest testing is]]))

(def conf {:linters {:self-requiring-namespace {:level :warning}}})

(deftest self-requiring-namespace-test
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 19, :level :warning, :message "Namespace is requiring itself: foo"} {:file "<stdin>", :row 1, :col 25, :level :warning, :message "Namespace is requiring itself: foo"} {:file "<stdin>", :row 1, :col 25, :level :warning, :message "duplicate require of foo"})
   (lint! "(ns foo (:require [foo] foo))" conf))
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 32,
     :level :warning,
     :message "duplicate require of foo"}]
   (lint! "(ns foo (:require-macros [foo] foo))" conf "--lang" "cljs")))
