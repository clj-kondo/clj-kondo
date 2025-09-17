(ns clj-kondo.get-on-key-test
  (:require
   [clj-kondo.test-utils :as tu :refer [lint! assert-submaps2]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest get-on-key-test
  (is (empty? (lint! "(get {:x 10} :x)" {:linters {:get-on-key {:level :warning}}})))
  (is (empty? (lint! "(-> {:x 10} (get :x))" {:linters {:get-on-key {:level :warning}}})))
  (is (empty? (lint! "(get [1 2 3] 1)" {:linters {:get-on-key {:level :warning}}})))
  (is (empty? (lint! "(-> [1 2 3] (get 1))" {:linters {:get-on-key {:level :warning}}})))
  (assert-submaps2
   [{:row 1,
     :col 1,
     :level :warning,
     :message "Get called on a key."}]
   (lint! "(get :x {:x 10})" {:linters {:get-on-key {:level :warning}}}))
  (assert-submaps2
   [{:row 1,
     :col 1,
     :level :warning,
     :message "Get called on a key."}]
   (lint! "(get 1 [1 2 3])" {:linters {:get-on-key {:level :warning}}})))

