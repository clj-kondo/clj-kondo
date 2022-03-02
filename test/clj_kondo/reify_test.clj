(ns clj-kondo.reify-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps]]
            [clojure.test :as t :refer [deftest is]]))

(deftest reify-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 36, :level :warning, :message "unused binding x"})
   (lint! "(reify clojure.lang.IDeref (deref [x] nil))"
          {:linters {:unused-binding {:level :warning}}}))
  (is (empty? (lint! "(reify clojure.lang.IDeref (deref [_] nil))"
                     {:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "
(ns fiddle
  (:import (java.lang.management PlatformManagedObject)
           (javax.management ObjectName)))

(reify PlatformManagedObject
  (^ObjectName getObjectName [_this]))"
                     {:linters {:unused-import {:level :warning}}}))))
