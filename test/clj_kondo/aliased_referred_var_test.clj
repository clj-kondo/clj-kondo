(ns clj-kondo.aliased-referred-var-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps2]]
            [clojure.test :refer [deftest is testing]]))

(def default-config {:linters {:unused-referred-var {:level :off}
                               :unused-namespace {:level :off}
                               :aliased-referred-var {:level :warning}}})

(deftest aliased-referred-var-test
  (testing "warns when var is used both via referral and alias"
    (assert-submaps2 [{:row 2
                       :col 1
                       :level :warning
                       :message "var clojure.set/union is referred as union but also used via alias set"}]
                     (lint! "(ns foo (:require [clojure.set :as set :refer [union]]))
(set/union #{1} #{2})" default-config)))
  (testing "does not warn when only referred name is used"
    (is (empty? (lint! "(ns foo (:require [clojure.set :as set :refer [union]]))
(union #{3} #{4})" default-config))))
  (testing "does not warn when only aliased name is used"
    (is (empty? (lint! "(ns foo (:require [clojure.set :as set]))
(set/union #{1} #{2})" default-config)))))

(deftest multiple-aliased-usages-test
  (testing "warns for every aliased usage"
    (assert-submaps2 [{:row 2
                       :col 1
                       :level :warning
                       :message "var clojure.set/union is referred as union but also used via alias set"}
                      {:row 3
                       :col 1
                       :level :warning
                       :message "var clojure.set/union is referred as union but also used via alias set"}]
                     (lint! "(ns foo (:require [clojure.set :as set :refer [union]]))
(set/union #{1} #{2})
(set/union #{1} #{2})" default-config))))
