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
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 1, :level :warning,
      :message "Unresolved namespace foo. Are you missing a require?"})
   (lint! "::foo/x"))
  (assert-submaps
    '({:file "<stdin>", :row 1, :col 2, :level :warning,
       :message "Unresolved namespace foo. Are you missing a require?"})
    (lint! "(foo/x) (foo/x)"))
  (assert-submaps
    '({:file "<stdin>", :row 1, :col 2, :level :warning,
       :message "Unresolved namespace foo. Are you missing a require?"}
      {:file "<stdin>", :row 1, :col 10, :level :warning,
       :message "Unresolved namespace foo. Are you missing a require?"})
    (lint! "(foo/x) (foo/x)" {:linters {:unresolved-namespace {:report-duplicates true}}}))
  ;; avoiding false positives
  (is (empty? (lint! (io/file "project.clj"))))
  (is (empty? (lint! "js/foo" "--lang" "cljs")))
  (is (empty? (lint! "goog/foo" "--lang" "cljs")))
  (is (empty? (lint! "(java.lang.Foo/Bar)")))
  (is (empty? (lint! "(clojure.core/inc 1)")))
  (is (empty? (lint! "(comment (require '[foo.bar]) (foo.bar/x))")))
  (is (empty? (lint! "(Math/pow 2 3)" "--lang" "cljs")))
  (is (empty? (lint! "(System/exit 0)")))
  (is (empty? (lint! "(require '[foo.bar] '[clojure.string :as str]) (str/starts-with? \"foo\" \"bar\")")))
  (is (empty? (lint! "(ns foo (:import java.util.regex.Pattern)) (Pattern/compile \"foo\")")))
  (is (empty? (lint! "(ns foo (:require [foo.bar])) (foo.bar$macros/x)"
                     "--lang" "cljs")))
  (is (empty? (lint! "
(ns foo
  {:clj-kondo/config {:linters {:unresolved-namespace {:level :off}}}})

x/bar ;; <- no warning")))
  (is (empty? (lint! "
(ns foo
  {:clj-kondo/config '{:linters {:unresolved-namespace {:exclude [criterium.core]}}}})
(criterium.core/quick-bench nil)
")))
  (is (empty? (lint! "
(do (require '[clojure.string :as str])
    (str/join [1 2 3]))
")))
  (is (empty? (lint! "
(require (quote [clojure.string :as str]))
(str/join 1 [1 2 3])
")))
  (is
   (empty?
    (lint! "(foo-db/dude)"
           '{:ns-groups [{:name db :pattern "-db$"}]
             :linters {:unresolved-namespace {:exclude [db]}}}))))
