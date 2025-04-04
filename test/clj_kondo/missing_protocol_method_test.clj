(ns clj-kondo.missing-protocol-method-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps2]]
            [clojure.test :refer [deftest is testing]]))

(deftest single-alias-test
  (assert-submaps2 '({:file "<stdin>", :row 8, :col 3, :level :warning,
                      :message "Missing protocol method(s): bar"})
                   (lint! "(ns scratch)

(defprotocol IDude
  (foo [_])
  (bar [_]))

(defrecord Dude []
  IDude
  (foo [_]))
")))
