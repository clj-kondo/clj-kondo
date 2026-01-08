(ns clj-kondo.redundant-format-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps2]]
   [clojure.test :refer [deftest is testing]]))

(def config {:linters {:redundant-format {:level :warning}}})

(deftest redundant-format-test
  (testing "format with no placeholders"
    (assert-submaps2
     [{:level :warning :message "Format string contains no format specifiers"}]
     (lint! "(format \"hello\")" config))
    (assert-submaps2
     [{:level :warning :message "Format string contains no format specifiers"}]
     (lint! "(format \"hello world\")" config)))
  
  (testing "printf with no placeholders"
    (assert-submaps2
     [{:level :warning :message "Format string contains no format specifiers"}]
     (lint! "(printf \"hello\")" config)))
  
  (testing "errorf with no placeholders"
    (assert-submaps2
     [{:level :warning :message "Format string contains no format specifiers"}]
     (lint! "(require '[clojure.tools.logging :as log]) (log/errorf \"hello\")" config)))
  
  (testing "infof with no placeholders"
    (assert-submaps2
     [{:level :warning :message "Format string contains no format specifiers"}]
     (lint! "(require '[clojure.tools.logging :as log]) (log/infof \"hello\")" config)))
  
  (testing "logf with no placeholders"
    (assert-submaps2
     [{:level :warning :message "Format string contains no format specifiers"}]
     (lint! "(require '[clojure.tools.logging :as log]) (log/logf :info \"hello\")" config)))
  
  (testing "format with placeholders should not warn"
    (is (empty? (lint! "(format \"hello %s\" \"world\")" config))))
  
  (testing "format with %% and %n only should warn (these are escaped)"
    (assert-submaps2
     [{:level :warning :message "Format string contains no format specifiers"}]
     (lint! "(format \"hello%%world%n\")" config)))
  
  (testing "linter is :info by default"
    (is (assert-submaps2
         [{:level :info :message "Format string contains no format specifiers"}]
         (lint! "(format \"hello\")"))))
  
  (testing "respects :off level"
    (is (empty? (lint! "(format \"hello\")"
                       (assoc-in config [:linters :redundant-format :level] :off)))))
  
  (testing "multiple format calls"
    (assert-submaps2
     [{:level :warning :message "Format string contains no format specifiers"}
      {:level :warning :message "Format string contains no format specifiers"}]
     (lint! "(format \"hello\") (format \"world\")" config)))
  
  (testing "errorf with throwable and no placeholders"
    (assert-submaps2
     [{:level :warning :message "Format string contains no format specifiers"}]
     (lint! "(require '[clojure.tools.logging :as log]) (log/errorf (ex-info \"\" {}) \"hello\")" config)))
  
  (testing "logf with throwable and no placeholders"
    (assert-submaps2
     [{:level :warning :message "Format string contains no format specifiers"}]
     (lint! "(require '[clojure.tools.logging :as log]) (log/logf :error (ex-info \"\" {}) \"hello\")" config))))
