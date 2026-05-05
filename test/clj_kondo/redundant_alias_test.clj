(ns clj-kondo.redundant-alias-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :as t :refer [deftest is testing]]))

(def config {:linters {:redundant-alias {:level :warning}}})

(deftest redundant-alias-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 28, :level :warning, :message "redundant alias: bar"})
   (lint! "(ns foo (:require [bar :as bar])) (bar/baz 42)" config))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 32, :level :warning, :message "redundant alias: bar.baz"})
   (lint! "(ns foo (:require [bar.baz :as bar.baz])) (bar.baz/qux 42)" config))
  (is (empty? (lint! "(ns foo (:require [\"bar\" :as bar])) (bar/qux 42)" config)))
  (is (empty? (lint! "(ns foo (:require [bar :as baz])) (baz/qux 42)" config)))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 8, :level :warning, :message "redundant alias: bar"})
   (lint! "(alias 'bar 'bar)" config))
  (is (empty? (lint! "(alias 'bar 'baz)" config)))
  (is (empty? (lint! "(let [bar 'bar, baz 'baz]) (alias bar baz)" config)))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 20, :level :warning, :message "redundant alias: bar"})
   (lint! "(require '[bar :as bar]) (bar/baz 42)" config))
  (is (empty? (lint! "(require '[bar :as baz]) (baz/qux 42)" config)))
  (is (empty? (lint! "(require '[bar [baz :as baz]]) (baz/qux 42)" config))))
