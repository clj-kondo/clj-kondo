(ns clj-kondo.missing-protocol-method-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps2]]
            [clojure.test :refer [deftest is testing]]))

(deftest single-alias-test
  (assert-submaps2 '({:file "<stdin>", :row 8, :col 3, :level :warning, :message "Missing protocol method(s): foo, bar"}
                     {:file "<stdin>", :row 11, :col 3, :level :warning, :message "Missing protocol method(s): bar"})
                   (lint! "(ns scratch)

(defprotocol IDude
  (foo [_])
  (bar [_]))

(defrecord Foo []
  IDude)

(defrecord Dude []
  IDude
  (foo [_]))
")))

;; TODO: extend-protocol
;; TODO: extend-type
;; TODO: deftype
;; TODO: reify

;; TODO: docs
