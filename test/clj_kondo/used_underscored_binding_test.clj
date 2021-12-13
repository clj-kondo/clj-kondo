(ns clj-kondo.used-underscored-binding-test
  (:require [clj-kondo.test-utils :refer [assert-submaps lint!]]
            [clojure.test :refer [deftest is testing]]
            [missing.test.assertions]))

(deftest used-underscored-binding-test
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 7,
      :level :warning,
      :message "used binding _x marked as unused."}
     {:file "<stdin>",
      :row 1,
      :col 29,
      :level :warning,
      :message "used binding _c marked as unused."})
   (lint! "(let [_x 0 {:keys [a b] :as _c} v]  [a b _x _c])"
          '{:linters {:used-underscored-binding {:level :warning}}}))
  (is (empty?  (lint! "(let [_x 0 {:keys [a b] :as _c} v]  [a b _x _c])"
                      '{:linters {:used-underscored-binding {:level :off}}})))
  (is (empty? (lint! "(doto (Object.) (.method))"
                     '{:linters {:used-underscored-binding {:level :warning}}})))
  (is (empty? (lint! "(let [_ 1] _)"
                     '{:linters {:used-underscored-binding {:level :warning}}}))))
