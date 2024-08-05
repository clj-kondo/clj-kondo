(ns clj-kondo.destructured-or-bindings-of-same-map-test
  (:require [clj-kondo.test-utils :as tu :refer [lint! assert-submaps2]]
            [clojure.test :refer [deftest is]]))

(deftest destructured-or-refers-bindings-of-same-map-test
  (is (assert-submaps2
       '({:file "<stdin>", :row 1, :col 52, :level :warning, :message "Destructured :or refers to binding of same map: user"})
       (lint! "(defn get-email [{email :email :as user :or {email user}}] email)"
              {:linters {:unresolved-symbol {:level :error}}}))))
