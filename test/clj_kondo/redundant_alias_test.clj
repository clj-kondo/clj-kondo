(ns clj-kondo.redundant-alias-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest redundant-alias-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 28, :level :warning, :message "redundant alias: bar"})
   (lint! "(ns foo (:require [bar :as bar])) (bar/baz 42)"))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 32, :level :warning, :message "redundant alias: bar.baz"})
   (lint! "(ns foo (:require [bar.baz :as bar.baz])) (bar.baz/qux 42)"))
  (is (empty? (lint! "(ns foo (:require [\"bar\" :as bar])) (bar/qux 42)")))
  (is (empty? (lint! "(ns foo (:require [bar :as baz])) (baz/qux 42)")))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 8, :level :warning, :message "redundant alias: bar"})
   (lint! "(alias 'bar 'bar)"))
  (is (empty? (lint! "(alias 'bar 'baz)")))
  (is (empty? (lint! "(let [bar 'bar, baz 'baz]) (alias bar baz)")))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 20, :level :warning, :message "redundant alias: bar"})
   (lint! "(require '[bar :as bar]) (bar/baz 42)"))
  (is (empty? (lint! "(require '[bar :as baz]) (baz/qux 42)")))
  (is (empty? (lint! "(require '[bar [baz :as baz]]) (baz/qux 42)"))))
