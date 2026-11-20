(ns clj-kondo.duplicate-require-key-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is testing]]))

(deftest duplicate-require-key-test
  (testing "duplicate :refer key in ns require"
    (assert-submaps2
     '[{:row 2, :col 41, :level :warning, :message "Duplicate require option: :refer. Only the last value will be used."}]
     (lint! "(ns foo
  (:require [clojure.set :refer [union] :refer [difference]]))
(difference #{:a} #{:b})"
            "{:linters {:unused-namespace {:level :off}
                        :unused-referred-var {:level :off}
                        :refer {:level :off}}}")))

  (testing "duplicate :as key in ns require"
    (assert-submaps2
     '[{:row 2, :col 32, :level :warning, :message "Duplicate require option: :as. Only the last value will be used."}]
     (lint! "(ns foo
  (:require [clojure.set :as s :as str]))
(str/union #{:a} #{:b})"
            "{:linters {:unused-namespace {:level :off}}}")))

  (testing "multiple duplicate keys"
    (assert-submaps2
     '[{:row 2, :col 41, :level :warning, :message "Duplicate require option: :refer. Only the last value will be used."}
       {:row 2, :col 67, :level :warning, :message "Duplicate require option: :as. Only the last value will be used."}]
     (lint! "(ns foo
  (:require [clojure.set :refer [union] :refer [difference] :as s :as str]))"
            "{:linters {:unused-namespace {:level :off}
                        :unused-referred-var {:level :off}
                        :refer {:level :off}}}")))

  (testing "no warning when keys are different"
    (is (empty? (lint! "(ns foo
  (:require [clojure.set :refer [union] :as s]))"
                       "{:linters {:unused-namespace {:level :off}
                                   :unused-referred-var {:level :off}
                                   :refer {:level :off}}}"))))

  (testing "duplicate :exclude key"
    (assert-submaps2
     '[{:row 2, :col 55, :level :warning, :message "Duplicate require option: :exclude. Only the last value will be used."}]
     (lint! "(ns foo
  (:require [clojure.set :refer :all :exclude [union] :exclude [intersection]]))"
            "{:linters {:unused-namespace {:level :off}
                        :refer-all {:level :off}}}")))

  (testing "duplicate :rename key"
    (assert-submaps2
     '[{:row 2, :col 56, :level :warning, :message "Duplicate require option: :rename. Only the last value will be used."}]
     (lint! "(ns foo
  (:require [clojure.set :refer :all :rename {union u} :rename {intersection i}]))"
            "{:linters {:unused-namespace {:level :off}
                        :refer-all {:level :off}}}")))

  (testing "duplicate require option in standalone require"
    (assert-submaps2
     '[{:row 1, :col 39, :level :warning, :message "Duplicate require option: :refer. Only the last value will be used."}]
     (lint! "(require '[clojure.set :refer [union] :refer [difference]])"
            "{:linters {:unused-namespace {:level :off}
                        :unused-referred-var {:level :off}
                        :refer {:level :off}}}")))

  (testing "duplicate :refer-macros key"
    (assert-submaps2
     '[{:row 2, :col 48, :level :warning, :message "Duplicate require option: :refer-macros. Only the last value will be used."}]
     (lint! "(ns foo
  (:require [clojure.core :refer-macros [defn] :refer-macros [defmacro]]))"
            "{:linters {:unused-namespace {:level :off}
                        :unused-referred-var {:level :off}
                        :refer {:level :off}}}")))

  (testing "linter can be disabled"
    (is (empty? (lint! "(ns foo
  (:require [clojure.set :refer [union] :refer [difference]]))"
                       "{:linters {:unused-namespace {:level :off}
                                   :unused-referred-var {:level :off}
                                   :refer {:level :off}
                                   :duplicate-require-key {:level :off}}}"))))

  (testing "linter can be disabled with inline ignore"
    (is (empty? (lint! "(ns foo
  #_:clj-kondo/ignore
  (:require [clojure.set :refer [union] :refer [difference]]))"
                       "{:linters {:unused-namespace {:level :off}
                                   :unused-referred-var {:level :off}
                                   :refer {:level :off}}}"))))

  (testing "linter can be disabled with inline ignore on specific require"
    (is (empty? (lint! "(ns foo
  (:require #_:clj-kondo/ignore [clojure.set :refer [union] :refer [difference]]))"
                       "{:linters {:unused-namespace {:level :off}
                                   :unused-referred-var {:level :off}
                                   :refer {:level :off}}}")))))
